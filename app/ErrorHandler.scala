import com.typesafe.scalalogging.LazyLogging
import org.webjars.play.WebJarsUtil
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper}
import views.html.notFound

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent._

@Singleton
class ErrorHandler @Inject()(implicit val webJarsUtil: WebJarsUtil,
                             environment: Environment,
                             config: Configuration,
                             sourceMapper: OptionalSourceMapper,
                             router: Provider[Router],
                             notFound: notFound)
  extends DefaultHttpErrorHandler(environment, config, sourceMapper, router)
    with LazyLogging {

  private val RobotRequests = Set(
    "N8kJ",
    "VLui",
    "8d2z",
    "/cob?ZiO9&link=pdf",
    "/a2billing",
    "/wp-json",
    "/fckeditor",
    ".php",
    ".ini",
    "oppfunds.do",
    ".well-known/",
    "mcd-cbo.do",
    "nasoya.do",
    "benadryl",
    "/tyson",
    "nba_logo.png",
    "/wp-includes/",
    "/wp-admin/",
    "/cgi-bin/",
    "sitemap.xml",
    "nsukey=",
    "/recordings/",
    "/philips-email/"
  )

  def isRobot: String => Boolean = uri =>
    RobotRequests.exists(uri.contains) || uri.endsWith(".sql") || uri.endsWith(".sql.zip")

  def isFavicon: String => Boolean = path => path.endsWith("favicon.ico") || path.endsWith("favicon.png")

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    val uri = request.uri
    val path = request.path
    val httpMethod = request.method
    val isGetMethod = httpMethod.equalsIgnoreCase("get")
    val isOptionsMethod = httpMethod.equalsIgnoreCase("options")
    val isHeadMethod = httpMethod.equalsIgnoreCase("head")

    val isAppleTouchResource = path.matches("""^.*/apple-touch-icon.*\.png$""")

    val shouldIgnore = isRobot(uri) || isFavicon(path) || isAppleTouchResource || isOptionsMethod || isHeadMethod
    val shouldRetryWithSlash = isGetMethod && !path.endsWith("/")
    logger.debug(s"shouldRetryWithSlash: $shouldRetryWithSlash")
    if (shouldIgnore) {
      Future.successful(Ok(notFound()))
    } else if (shouldRetryWithSlash) {
      logger.info(s"PageNotFound: $request  Redirecting with /")
      Future.successful(Results.Redirect(s"$path/", request.queryString))
    } else {
      Future.successful(Ok(notFound()))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      InternalServerError("")
    )
  }

  override def onForbidden(request: RequestHeader, message: String): Future[Result] = {
    logger.info(s"ForbiddenRequest: ${request.uri}")
    Future.successful(
      Forbidden("You're not allowed to access this resource.")
    )
  }
}
