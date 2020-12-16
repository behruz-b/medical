//import javax.inject.{Inject, Singleton}
//import org.webjars.play.WebJarsUtil
//import play.api.http.HttpErrorHandler
//import play.api.mvc.Results._
//import play.api.mvc._
//import views.html.errors.notFoundPage
//
//import scala.concurrent._
//
//@Singleton
//class ErrorHandler  @Inject()(implicit val webJarsUtil: WebJarsUtil,
//                              notFound: notFoundPage
//                             ) extends HttpErrorHandler {
//
//  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
//    Future.successful{
//      Ok(notFound())
//    }
//  }
//
//  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
//    Future.successful(
//      InternalServerError("")
//    )
//  }
//}
