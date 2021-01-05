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
  val loginKey = "patient_key"

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
      val patient = Patient(LocalDateTime.now, firstName, lastName, phone, email.some, passportSN, generateCustomerId, generateLogin, generatePassword)
      (patientManager ? CreatePatient(patient)).mapTo[Either[String, String]].map {
        case Right(_) =>
          logger.debug(s"SUCCEESS")
          Ok(Json.toJson(patient.customer_id))
        case Left(e) =>
          logger.debug(s"ERROR")
          BadRequest(e)
      }.recover {
        case e =>
          logger.error("Error while creating patient", e)
          BadRequest("Error")
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
    request.session.get(loginKey) match {
      case Some(_) => Future.successful(BadRequest("You are already authorized"))
      case None =>
        val body = request.body.asFormUrlEncoded
        val login = body.get("adminName").flatMap(_.headOption)
        val password = body.get("adminPass").flatMap(_.headOption)
        if (login.exists(_.nonEmpty) || password.exists(_.nonEmpty)) {
          (patientManager ? GetPatientByLogin(login.get, password.get)).mapTo[Either[String, String]].map {
            case Right(_) =>
              Redirect(routes.HomeController.index("uz")).addingToSession(loginKey -> login.get)
            case Left(error) =>
              BadRequest(error)
          }.recover {
            case error: Throwable =>
              logger.error(s"Error occurred while check user: $error")
              BadRequest("Authorization failed")
          }
        } else {
          Future.successful(BadRequest("Login or Password undefined"))
        }
    }
  }

  def logout: Action[AnyContent] = Action { implicit request =>
    request.session.get(loginKey) match {
      case Some(_) => Redirect(routes.HomeController.index("uz")).withSession(request.session - loginKey)
      case None => BadRequest("You are not authorized")
    }
  }

  private def generateCustomerId = randomStr(1).toUpperCase + "-" + getRandomDigits(3)

  private def generateLogin = randomStr(1).toUpperCase + "-" + getRandomDigit(3)

  private def generatePassword = randomStr(1).toUpperCase + "-" + getRandomDigit(3)
}