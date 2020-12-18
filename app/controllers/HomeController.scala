package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import javax.inject._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import views.html._
import protocols.AppProtocol._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val configuration: Configuration,
                               implicit val webJarsUtil: WebJarsUtil,
                               addPersonToOrder: registration,
                               indexTemplate: index,
                               @Named("user-manager") val userManager: ActorRef)
                              (implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)

  def index: Action[AnyContent] = Action {
    Ok(indexTemplate())
  }

  def addPerson(): Action[AnyContent] = Action {
    Ok(addPersonToOrder())
  }

  def createUser: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val firstname = (request.body \ "firstName").as[String]
    val lastname = (request.body \ "lastName").as[String]
    val passportSN = (request.body \ "passportSN").as[String]
    val phone = (request.body \ "phone").as[String]
    val email = (request.body \ "email").as[String]
    val patient = User(firstname, lastname, passportSN, phone, email.some)
    (userManager ? CreateNewUser(patient)).mapTo[User].map { user =>
      Ok(Json.toJson(s"Siz ${user.firstName} muvaffaqiyatli ro'yxatdan utdingiz!"))
    }
  }

}