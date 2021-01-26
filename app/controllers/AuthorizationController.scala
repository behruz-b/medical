package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import play.api.mvc._
import protocols.Authentication.{Login, LoginFormWithClientCode, loginPatterns, loginPlayFormWithClientCode}
import protocols.UserProtocol.CheckUserByLogin

import java.net.URL
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthorizationController @Inject()(val controllerComponents: ControllerComponents,
                                        @Named("user-manager") val userManager: ActorRef)
                                        (implicit val ec: ExecutionContext)
  extends BaseController with Auth {
  implicit val defaultTimeout: Timeout = Timeout(30.seconds)

  def basePathExtractor(implicit request: RequestHeader): String = {
    val referUrl = request.headers.get(REFERER).get
    val url = new URL(referUrl)
    url.getPath
  }

  def authInit(sessionAttrName: String,
               sessionAttrVal: String,
               sessionDuration: Option[FiniteDuration] = None): Seq[(String, String)] = {
    val expiresAtSessionAttr = expiresAtSessionAttrName(sessionAttrName)
    sessionDuration.foldLeft(Map(sessionAttrName -> sessionAttrVal)) { (acc, sessionDur) =>
      val nextExpiration = System.currentTimeMillis() + sessionDur.toMillis
      acc + (expiresAtSessionAttr -> nextExpiration.toString)
    }.toSeq
  }

  private def checkLogin(login: String, password: String, uri: String, loginParams: Login)
                        (implicit request: RequestHeader): Future[Result] = {
    (userManager ? CheckUserByLogin(login, password)).mapTo[Either[String, String]].map {
      case Right(role) =>
        Redirect(loginParams.redirectUrl)
          .addingToSession(authInit(loginParams.sessionKey, role, loginParams.sessionDuration): _*)
      case Left(error) =>
        logger.error("Error occurred while check login and password", error)
        Redirect(uri).flashing("error" -> "Login or password incorrect!")
    }
  }

  private def checkLoginPassword(login: String, password: String, uri: String)
                                (implicit request: RequestHeader): Future[Result] = {
    loginPatterns.get(uri) match {
      case Some(value) =>
        checkLogin(login, password, uri, value)
      case None =>
        logger.info(s"unknown uri: $uri")
        Future.successful(Redirect(uri).flashing("error" -> "Something went wrong. Please try again."))
    }
  }

  private def checkLoginPost(login: String, password: String, referUrl: String)
                            (implicit request: Request[AnyContent]): Future[Result] = {
    checkLoginPassword(login, password, basePathExtractor).recover {
      case error =>
        logger.error("error", error)
        Redirect(referUrl).flashing("error" -> "Something went wrong. Please try again.")
    }
  }

  def loginPost: Action[AnyContent] = Action.async { implicit request =>
    val referUrl = request.headers.get(REFERER).get
    loginPlayFormWithClientCode.bindFromRequest().fold(
      errorForm => {
        logger.info(s"errorForm: $errorForm")
        Future.successful(Redirect(referUrl).flashing("error" -> "Please enter login and password"))
      }, {
        case LoginFormWithClientCode(login, password) =>
          checkLoginPost(login, password, referUrl)
      }
    )
  }

  def logout: Action[AnyContent] = Action { implicit request =>
    val urlPath = if (request.headers.get(REFERER).isDefined) {
      basePathExtractor
    } else {
      request.uri
    }
    if (request.session.isEmpty) {
      BadRequest("You are not authorized")
    } else {
      val loginParams = loginPatterns(urlPath.replaceFirst("logout", ""))
      val redirectUrl = loginParams.redirectUrl
      Redirect(redirectUrl).withSession(request.session - loginParams.sessionKey)
    }
  }

}
