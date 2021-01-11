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
                               @Named("patient-manager") val patientManager: ActorRef)
                              (implicit val webJarsUtil: WebJarsUtil, implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging with CommonMethods {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  val loginKey = "patient_key"
  val tempFilesPath: String = configuration.get[String]("analaysis_folder")

  def index(language: String): Action[AnyContent] = Action {
    Ok(indexTemplate(language))
  }

  def logout: Action[AnyContent] = Action { implicit request =>
    request.session.get(loginKey) match {
      case Some(_) => Redirect(routes.HomeController.index()).withSession(request.session - loginKey)
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
    request.session.get(loginKey).fold(Redirect(routes.HomeController.login())){ _ =>
      Ok(addAnalysisResultPageTemp(language))
    }
  }

  def getPatients: Action[AnyContent] = Action.async { implicit request =>
    request.session.get(loginKey).fold(Future.successful(Unauthorized(Json.toJson("You are not authorized")))) { _ =>
      (patientManager ? GetPatients).mapTo[List[Patient]].map { patients =>
        Ok(Json.toJson(patients))
      }
    }
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { request =>
    request.body
      .file("file")
      .map { picture =>
        // only get the last part of the filename
        // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
        val filename    = Paths.get(picture.filename).getFileName
        val fileSize    = picture.fileSize
        val contentType = picture.contentType

        val body = request.body.asFormUrlEncoded
        val customerId = body.get("id").flatMap(_.headOption)
        logger.debug(s"filename: $filename")
        logger.debug(s"fileSize: $fileSize")
        logger.debug(s"contentType: $contentType")

        // need to create folder "patients_results" out of the project
        val time_stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())
        val path = tempFilesPath + "/" + customerId.getOrElse("")
        picture.ref.copyTo(Paths.get(s"$path" + "_" + s"$time_stamp.png"), replace = true)
        Ok("File uploaded")
      }
      .getOrElse {
        Redirect(routes.HomeController.index()).flashing("error" -> "Missing file")
      }
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
              Redirect(routes.HomeController.index()).addingToSession(loginKey -> login.get)
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