package controllers

import akka.actor.ActorSystem
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import protocols.Authentication
import protocols.Authentication.loginParams

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

trait Auth extends LazyLogging {
  implicit val actorSystem: ActorSystem
  implicit private val ex: ExecutionContext = actorSystem.dispatcher

  private def expiresAtSessionAttrName: String => String = _ + ".exp"
  private def loginParam: String => Option[Authentication.Login] = loginParams.get

  object ErrorText {
    val SessionExpired = "Session expired. Please log in."
    val Unauthorized = "Unauthorized. Please log in."
  }

  private def roleSessionKey(implicit request: RequestHeader): String = {
    val companyCode = request.host
    loginParams.get(companyCode).fold {
      logger.error(s"Error occurred while get login param: $companyCode")
      ""
    }(_.sessionKey)
  }

  private def unauthorized[A: TypeTag]: A = typeOf[A] match {
    case t if t =:= typeOf[Result] => Unauthorized(ErrorText.Unauthorized).asInstanceOf[A]
    case t if t <:< typeOf[Future[Result]] => Future.successful(Unauthorized(ErrorText.Unauthorized)).asInstanceOf[A]
  }

  def authByRole(role: String)
                (result: => Result)
                (implicit request: RequestHeader): Result = {
    request.session.get(roleSessionKey) match {
      case Some(userRole) =>
        if (userRole == role) {
          checkAuth(roleSessionKey, loginParam(request.host).flatMap(_.sessionDuration))(result)
        } else {
          unauthorized
        }
      case None =>
        unauthorized
    }
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
      if (sessionDuration.isDefined && session.get(expiresAtAttr).isEmpty) {
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
