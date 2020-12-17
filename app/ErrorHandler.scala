import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.http.HttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import views.html.notFound

import scala.concurrent._

@Singleton
class ErrorHandler @Inject()(implicit val webJarsUtil: WebJarsUtil,
                             notFound: notFound
                            ) extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful {
      Ok(notFound())
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      InternalServerError("")
    )
  }
}
