package controllers
import java.util.Date
import java.time._
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging

import javax.inject._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import protocols.AppProtocol._
import protocols.UserProtocol.CheckUserByLogin
import views.html._

import java.nio.file.Paths
import java.text.SimpleDateFormat
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               indexTemplate: index,
                               loginPage: admin.login,
                               configuration: Configuration,
                               addAnalysisResultPageTemp: addAnalysisResult.addAnalysisResult,
                               @Named("patient-manager") val patientManager: ActorRef,
                               @Named("user-manager") val userManager: ActorRef)
                              (implicit val webJarsUtil: WebJarsUtil, implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging with CommonMethods {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  val LoginKey = "login_session_key"
  val DoctorLoginKey = "doctor_role"
  val RegLoginKey = "reg_role"
  val tempFilesPath: String = configuration.get[String]("analysis_folder")

  def index(language: String): Action[AnyContent] = Action { implicit request =>
    request.session.get(LoginKey).fold(Redirect(routes.HomeController.login())) { _ =>
      logger.debug(s"request: ${request.uri}")
      Ok(indexTemplate(language))
    }
  }

  def logout: Action[AnyContent] = Action { implicit request =>
    request.session.get(LoginKey) match {
      case Some(_) => Redirect(routes.HomeController.index()).withSession(request.session - LoginKey)
      case None => BadRequest("You are not authorized")
    }
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

  def login(language: String): Action[AnyContent] = Action {
    Ok(loginPage(language))
  }

  def addAnalysisResult(language: String): Action[AnyContent] = Action { implicit request =>
    request.session.get(LoginKey).fold(Redirect(routes.HomeController.login())){ role_key =>
      if (role_key == DoctorLoginKey) {
        Ok(addAnalysisResultPageTemp(language))
      } else {
        Unauthorized("You haven't got right role to see page")
      }
    }
  }

  def getPatients: Action[AnyContent] = Action.async { implicit request =>
    request.session.get(LoginKey).fold(Future.successful(Unauthorized(Json.toJson("You are not authorized")))) { _ =>
      (patientManager ? GetPatients).mapTo[List[Patient]].map { patients =>
        Ok(Json.toJson(patients))
      }
    }
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    request.body
      .file("file")
      .map { picture =>
        val body = request.body.asFormUrlEncoded
        body.get("id").flatMap(_.headOption) match {
          case Some(customerId) =>
            // need to create folder "patients_results" out of the project
            val time_stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())
            val analysisFileName = customerId  + "_" +  time_stamp + ".png"
            picture.ref.copyTo(Paths.get(tempFilesPath + "/" + analysisFileName), replace = true)
            (patientManager ? AddAnalysisResult(customerId, analysisFileName)).mapTo[Either[String, String]].map {
              case Right(_) =>
                logger.debug(s"SUCCEESS")
                Redirect("/doc").flashing("success" -> "Fayl yuklandi!")
              case Left(e) =>
                logger.error(s"ERROR, e: $e")
                Redirect("/doc").flashing("error" -> "Ma'lumotlar bazasiga yozishda xatolik yuz berdi")
            }.recover {
              case error: Throwable =>
                logger.error("Error while creating image", error)
                Redirect("/doc").flashing("error" -> "Something went wrong")
            }
          case None =>
            Future.successful(Redirect("/doc").flashing("error" -> "Ma'lumotlar bazasidan bunday ID topilmadi"))
        }
      }.getOrElse {
        logger.debug(s"No file to upload")
        Future.successful(Redirect(routes.HomeController.index()).flashing("error" -> "Missing file"))
      }
  }

  def loginPost: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    request.session.get(LoginKey) match {
      case Some(loginKey) =>
        loginKey match {
          case DoctorLoginKey => Future.successful(Redirect("/doc"))
          case RegLoginKey => Future.successful(Redirect("/reg"))
          case _ => Future.successful(Unauthorized("Your haven't got right Role"))
        }
      case None =>
        val body = request.body.asFormUrlEncoded
        val login = body.get("adminName").flatMap(_.headOption)
        val password = body.get("adminPass").flatMap(_.headOption)
        if (login.exists(_.nonEmpty) || password.exists(_.nonEmpty)) {
          (userManager ? CheckUserByLogin(login.get, password.get)).mapTo[Either[String, String]].map {
            case Right(role) =>
              role match {
                case "doctor" => Redirect("/doc").addingToSession(LoginKey -> DoctorLoginKey)
                case "reg" => Redirect("/reg").addingToSession(LoginKey -> RegLoginKey)
                case _ => Unauthorized("Your haven't got right Role")
              }
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

  private def generateCustomerId = randomStr(1).toUpperCase + "-" + getRandomDigits(3)

  private def generateLogin = randomStr(1).toUpperCase + "-" + getRandomDigit(3)

  private def generatePassword = randomStr(1).toUpperCase + "-" + getRandomDigit(3)
}