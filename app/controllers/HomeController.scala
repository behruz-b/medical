package controllers

import java.util.Date

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import javax.inject._
import org.webjars.play.WebJarsUtil
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import protocols.AppProtocol._
import views.html._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               implicit val webJarsUtil: WebJarsUtil,
                               addPersonToOrder: registration,
                               adminLoginTemplate: admin.login,
                               indexTemplate: index,
                               @Named("patient-manager") val patientManager: ActorRef)
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
    val firstName = (request.body \ "firstName").as[String]
    val lastName = (request.body \ "lastName").as[String]
    val passportSN = (request.body \ "passportSN").as[String]
    val phone = (request.body \ "phone").as[String]
    val email = (request.body \ "email").as[String]
    val login = (request.body \ "login").as[String]
    val password = (request.body \ "password").as[String]
    val patient = Patient(new Date(), firstName, lastName, phone, email.some, passportSN, login, passportSN)
    (patientManager ? CreatePatients(patient)).mapTo[Patient].map { patient =>
      Ok(Json.toJson(patient.customerId))
    }
  }

  def adminLogin: Action[AnyContent] = Action {
    Ok(adminLoginTemplate())
  }

}