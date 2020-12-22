package controllers

import java.time._

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

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.Try

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               implicit val webJarsUtil: WebJarsUtil,
                               addPersonToOrder: registration,
                               adminLoginTemplate: admin.login,
                               indexTemplate: index,
                               thankYouPageTemplate: thankYou,
                               @Named("patient-manager") val patientManager: ActorRef)
                              (implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging with CommonMethods {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)

  def index: Action[AnyContent] = Action {
    Ok(indexTemplate())
  }

  def thanks: Action[AnyContent] = Action {
    Ok(thankYouPageTemplate())
  }

  def addPerson(): Action[AnyContent] = Action {
    Ok(addPersonToOrder())
  }

  def createUser: Action[JsValue] = Action.async(parse.json) { implicit request =>
    Try {
      val firstName = (request.body \ "firstName").as[String]
      val lastName = (request.body \ "lastName").as[String]
      val passportSN = (request.body \ "passportSN").as[String]
      val phone = (request.body \ "phone").as[String]
      val email = (request.body \ "email").as[String]
      val login = (request.body \ "login").as[String]
      val password = (request.body \ "password").as[String]
      val patient = Patient(LocalDateTime.now, firstName, lastName, phone, email.some, passportSN, login, password, generateCustomerId.some)
      (patientManager ? CreatePatients(patient)).mapTo[Patient].map { patient =>
        Ok(Json.toJson(patient.customerId))
      }.recover {
        case error =>
          logger.error("Error occurred while create patient. Error:", error)
          BadRequest("Ro'yhatdan o'tishda xatolik yuz berdi. Iltimos qaytadan harakat qilib ko'ring!")
      }
    }.getOrElse(Future.successful(BadRequest("So'rovda xatolik bor. Iltimos qaytadan harakat qilib ko'ring!")))
  }

  def adminLogin: Action[AnyContent] = Action {
    Ok(adminLoginTemplate())
  }

  private def generateCustomerId = randomStr(1).toUpperCase + "-" + getRandomDigits(7)

}