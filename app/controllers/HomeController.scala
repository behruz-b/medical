
package controllers

import akka.actor.ActorRef
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import javax.inject._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc._
import views.html._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val configuration: Configuration,
                               implicit val webJarsUtil: WebJarsUtil,
                               indexTemplate: index,
                               @Named("user-manager") val userManager: ActorRef)
                              (implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)

  def index: Action[AnyContent] = Action {
    Ok(indexTemplate())
  }

}