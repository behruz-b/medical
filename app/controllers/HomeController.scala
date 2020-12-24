package controllers

import java.time._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import javax.inject._
import org.webjars.play.WebJarsUtil
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import protocols.AppProtocol._
import views.html._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               indexTemplate: index,
                               adminLoginTemplate: admin.login,
                               @Named("patient-manager") val patientManager: ActorRef)
                              (implicit val webJarsUtil: WebJarsUtil, implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging with CommonMethods {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)

  def index(language: String = "uz"): Action[AnyContent] = Action {
    Ok(indexTemplate(language))
  }

  def createUser: Action[JsValue] = Action.async(parse.json) { implicit request =>
    Try {
      val firstName = (request.body \ "firstName").as[String]
      val lastName = (request.body \ "lastName").as[String]
      val passportSN = (request.body \ "passportSn").as[String]
      val phone = (request.body \ "phone").as[String]
      val email = (request.body \ "email").as[String]
      val patient = Patient(LocalDateTime.now, firstName, lastName, phone, email.some, passportSN, generateLogin, generatePassword, generateCustomerId.some)
      (patientManager ? CreatePatients(patient)).mapTo[Patient].map { patient =>
        Ok(Json.toJson(patient.customerId))
      }
    } match {
      case Success(res) => res
      case Failure(exception) =>
        logger.error("Error occurred while create patient. Error:", exception)
        Future.successful(BadRequest("Ro'yhatdan o'tishda xatolik yuz berdi. Iltimos qaytadan harakat qilib ko'ring!"))
    }
  }

  def adminLogin(language: String): Action[AnyContent] = Action {
    Ok(adminLoginTemplate(language))
  }

  def loginPost: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    val body = request.body.asFormUrlEncoded
    val login = body.get("adminName").flatMap(_.headOption)
    val password = body.get("adminPass").flatMap(_.headOption)
    Future.successful(Redirect(controllers.routes.HomeController.index("uz"))
      .addingToSession("login" -> s"$login", "pass" -> s"$password")) // TODO need to replace with sessionKey
  }

  private def generateCustomerId = randomStr(1).toUpperCase + "-" + getRandomDigits(3)

  private def generateLogin = randomStr(1).toUpperCase + "-" + getRandomDigit(3)

  private def generatePassword = randomStr(1).toUpperCase + "-" + getRandomDigit(3)
}