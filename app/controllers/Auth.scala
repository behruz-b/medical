package controllers

import akka.actor.ActorSystem
import cats.implicits._
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

trait Auth {
  implicit val actorSystem: ActorSystem
  implicit private val ex: ExecutionContext = actorSystem.dispatcher

  private def expiresAtSessionAttrName: String => String = _ + ".exp"

  object ErrorText {
    val SessionExpired = "Session expired. Please log in."
    val Unauthorized = "Unauthorized. Please log in."
  }

  //
  //  def RoleSessionKey(implicit request: RequestHeader): String = {
  //    val companyCode = request.host
  //    loginPatterns(companyCode).sessionAttr.roleSessionKey
  //  }

  private def unauthorized[A: TypeTag]: A = typeOf[A] match {
    case t if t =:= typeOf[Result] => Unauthorized(ErrorText.Unauthorized).asInstanceOf[A]
    case t if t <:< typeOf[Future[Result]] => Future.successful(Unauthorized(ErrorText.Unauthorized)).asInstanceOf[A]
  }

  def authByRole(role: String, sessionAttr: String, sessionDuration: Option[FiniteDuration] = None)
                (result: => Result)
                (implicit request: RequestHeader): Result = {
    request.session.get("") match {
      case Some(userRole) =>
        if (userRole == role) {
          checkAuth(sessionAttr, sessionDuration)(result)
        } else {
          unauthorized
        }
      case None =>
        unauthorized
    }
    checkAuth(sessionAttr, sessionDuration)(result)
  }

  private def checkAuth[A: TypeTag](sessionAttr: String, sessionDuration: Option[FiniteDuration] = None)
                                   (body: => A)
                                   (implicit request: RequestHeader): A = {
    val expiresAtAttr = expiresAtSessionAttrName(sessionAttr)
    val session = request.session

    def addSessionIfNecessary(result: Result): Result = {
      sessionDuration.map { sessionDur =>
        val currentTime = System.currentTimeMillis()
        val nextExpiration = currentTime + sessionDur.toMillis
        result.addingToSession(expiresAtAttr -> nextExpiration.toString)
      }.getOrElse(result)
    }

    val errorResultOpt: Option[Result] =
      if (session.get(sessionAttr).isEmpty || (sessionDuration.isDefined && session.get(expiresAtAttr).isEmpty)) {
        Unauthorized(ErrorText.Unauthorized).some
      } else if (sessionDuration.isDefined && session.get(expiresAtAttr).exists(_.toLong < System.currentTimeMillis())) {
        Unauthorized(ErrorText.SessionExpired).some
      } else {
        None
      }

    typeOf[A] match {
      case t if t =:= typeOf[Result] =>
        errorResultOpt.getOrElse(
          addSessionIfNecessary(body.asInstanceOf[Result])
        ).asInstanceOf[A]
      case t if t <:< typeOf[Future[Result]] =>
        errorResultOpt.map(Future.successful).getOrElse(
          body.asInstanceOf[Future[Result]].map(addSessionIfNecessary)
        ).asInstanceOf[A]
    }
  }
}
