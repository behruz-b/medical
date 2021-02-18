package controllers

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import protocols.Authentication
import protocols.Authentication.AppRole._
import protocols.Authentication.{LoginSessionKey, Login, loginPatterns}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

trait Auth extends LazyLogging {
  implicit val ec: ExecutionContext

  def expiresAtSessionAttrName: String => String = _ + ".exp"

  private def loginParam: String => Option[Authentication.Login] = loginPatterns.get
  def getUserLogin(implicit request: RequestHeader): String = request.session.get("login").getOrElse("")
  def getUserAgent(implicit request: RequestHeader): String = request.headers.get("User-Agent").getOrElse("")
  def getRemoteAddress(implicit request: RequestHeader): String = request.headers.get("Remote-Address").getOrElse("")
  object ErrorText {
    val SessionExpired = "Session expired. Please log in."
    val Unauthorized = "Unauthorized. Please log in."
  }

  def isManager(implicit request: RequestHeader): Boolean = {
    request.session.get(LoginSessionKey).exists(r => r.contains(ManagerRole) || r.contains(AdminRole))
  }

  def isAdmin(implicit request: RequestHeader): Boolean = {
    request.session.get(LoginSessionKey).exists(_.contains(AdminRole))
  }

  def isDoctor(implicit request: RequestHeader): Boolean = {
    request.session.get(LoginSessionKey).exists(r => r.contains(DoctorRole) || r.contains(AdminRole))
  }

  def isRegister(implicit request: RequestHeader): Boolean = {
    request.session.get(LoginSessionKey).exists(r => r.contains(RegRole) || r.contains(AdminRole))
  }

  private def baseUriExtractor(implicit request: RequestHeader): String = {
    val path = request.path
    List("stats/", "patient/", "doc/", "admin/", "reg/", "change-password/")
      .map(keyword => (path.indexOf(keyword), keyword.length))
      .find(_._1 != -1) match {
      case Some(t) => path.substring(0, t._1 + t._2)
      case None => path
    }
  }

  private def roleSessionKey(implicit request: RequestHeader): String = {
    loginParam(baseUriExtractor).fold {
      logger.error(s"Error occurred while get login param: $baseUriExtractor")
      ""
    }(_.sessionAttr.sessionKey)
  }

  private def unauthorized[T: TypeTag]: T = typeOf[T] match {
    case t if t =:= typeOf[Result] => Unauthorized(ErrorText.Unauthorized).asInstanceOf[T]
    case t if t <:< typeOf[Future[Result]] => Future.successful(Unauthorized(ErrorText.Unauthorized)).asInstanceOf[T]
  }

  def authByRole[T: TypeTag](hasAccess: Boolean)(result: => T)(implicit request: RequestHeader): T = {
    if (hasAccess) {
      checkAuth(roleSessionKey, loginParam(baseUriExtractor).flatMap(_.sessionDuration))(result)
    } else {
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
