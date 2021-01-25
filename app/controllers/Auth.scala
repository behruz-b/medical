package controllers

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import protocols.Authentication
import protocols.Authentication.loginPatters

import java.net.URL
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

trait Auth extends LazyLogging {
  implicit val ec: ExecutionContext

  def expiresAtSessionAttrName: String => String = _ + ".exp"

  def basePathExtractor(implicit request: RequestHeader): String = {
    val referUrl = request.headers.get("referer").get
    val url = new URL(referUrl)
    url.getPath
  }

  private def loginParam: String => Option[Authentication.Login] = loginPatters.get

  object ErrorText {
    val SessionExpired = "Session expired. Please log in."
    val Unauthorized = "Unauthorized. Please log in."
  }

  private def roleSessionKey(implicit request: RequestHeader): String = {
    logger.debug(basePathExtractor)
    loginPatters.get(basePathExtractor).fold {
      logger.error(s"Error occurred while get login param: $basePathExtractor")
      ""
    }(_.sessionKey)
  }

  private def unauthorized[T: TypeTag]: T = typeOf[T] match {
    case t if t =:= typeOf[Result] => Unauthorized(ErrorText.Unauthorized).asInstanceOf[T]
    case t if t <:< typeOf[Future[Result]] => Future.successful(Unauthorized(ErrorText.Unauthorized)).asInstanceOf[T]
  }

  def authByRole[T: TypeTag](role: String)
                (result: => T)
                (implicit request: RequestHeader): T = {
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

  def checkAuth[T: TypeTag](sessionAttr: String, sessionDuration: Option[FiniteDuration] = None)
                                   (body: => T)
                                   (implicit request: RequestHeader): T = {
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

    typeOf[T] match {
      case t if t =:= typeOf[Result] =>
        errorResultOpt.getOrElse(
          addSessionIfNecessary(body.asInstanceOf[Result])
        ).asInstanceOf[T]
      case t if t <:< typeOf[Future[Result]] =>
        errorResultOpt.map(Future.successful).getOrElse(
          body.asInstanceOf[Future[Result]].map(addSessionIfNecessary)
        ).asInstanceOf[T]
    }
  }
}
