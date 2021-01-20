package controllers
import java.util.Date
import java.time._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.implicits._
import cats.data.EitherT
import com.typesafe.scalalogging.LazyLogging
import javax.inject._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import protocols.PatientProtocol._
import protocols.UserProtocol.CheckUserByLogin
import views.html._
import views.html.statistic._
import java.nio.file.Paths
import java.text.SimpleDateFormat

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               indexTemplate: views.html.index,
                               loginPage: views.html.admin.login,
                               configuration: Configuration,
                               addAnalysisResultPageTemp: addAnalysisResult.addAnalysisResult,
                               statsActionTemp: statisticTemplete,
                               getPatientsTemp: patients.patientsTable,
                               @Named("patient-manager") val patientManager: ActorRef,
                               @Named("user-manager") val userManager: ActorRef,
                               @Named("stats-manager") val statsManager: ActorRef)
                              (implicit val webJarsUtil: WebJarsUtil, implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging with CommonMethods {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  val LoginKey = "login_session_key"
  val DoctorLoginKey = "doctor_role"
  val RegLoginKey = "reg_role"
  val tempFilesPath: String = configuration.get[String]("analysis_folder")
  val tempFolderPath: String = configuration.get[String]("temp_folder")

  def index(language: String): Action[AnyContent] = Action { implicit request =>
    request.session.get(LoginKey).fold(Redirect(routes.HomeController.login())) {
      case RegLoginKey => Ok(indexTemplate(language))
      case _ => Redirect(routes.HomeController.index()).withSession(request.session - LoginKey)
    }
  }

  def analysisResult(customerId: String): Action[AnyContent] = Action.async {
    (patientManager ? GetPatientByCustomerId(customerId.toUpperCase)).mapTo[Either[String, Patient]].map {
      case Right(patient) =>
        logger.debug(s"SUCCEESS")
        if (patient.analysis_image_name.isDefined) {
//          val fileBytes = java.nio.file.Files.readAllBytes(Paths.get(tempFilesPath).resolve(patient.analysis_image_name.get))
//
//          val directoryPath = new java.io.File("public/images")
//          directoryPath.mkdirs()
//          val tempFile = java.io.File.createTempFile("elegant_analysis_", ".jpg", directoryPath)
//          val fos = new java.io.FileOutputStream(tempFile)
//          fos.write(fileBytes)
          Ok.sendFile(new java.io.File(tempFilesPath + "/" + patient.analysis_image_name.get))
//          Ok(analysisResultTemplate(customerId, tempFile.getPath.replace("public/", "")))
        } else {
          logger.error("Error while getting analysis file name")
          BadRequest("Error")
        }
      case Left(e) =>
        logger.debug(s"ERROR")
        BadRequest(e)
    }.recover {
      case e =>
        logger.error("Error while getting patient", e)
        BadRequest("Error")
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
      val prefixPhone = "998"
      val email = (request.body \ "email").as[String]
      //      val company_code = (request.body \ "company_code").as[String]
      val company_code = request.host
      logger.debug(s"User agent: ${request.headers.get("User-Agent")}")
      logger.debug(s"IP-Address: ${request.headers.get("Remote-Address")}")
      logger.debug(s"companyCode: $company_code")
      val patient = Patient(LocalDateTime.now, firstName, lastName, prefixPhone + phone, email.some, passportSN, generateCustomerId,
        company_code, generateLogin, generatePassword)
      (patientManager ? CreatePatient(patient)).mapTo[Either[String, String]].map {
        case Right(_) =>
          val stats = StatsAction(LocalDateTime.now, request.host, action = "reg_submit",
            request.headers.get("Remote-Address").get, request.headers.get("User-Agent").get)
          statsManager ! AddStatsAction(stats)
          Ok(Json.toJson(patient.customer_id))
        case Left(e) =>
          logger.debug(s"ERROR")
          BadRequest(e)
      }.recover {
        case e: Any =>
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
    request.session.get(LoginKey).fold(Redirect(routes.HomeController.login())) { role_key =>
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

  def getPatientsTemplate(): Action[AnyContent] = Action {
    Ok(getPatientsTemp())
  }

  def getStats: Action[AnyContent] = Action.async { implicit request =>
    request.session.get(LoginKey).fold(Future.successful(Unauthorized(Json.toJson("You are not authorized")))) { _ =>
      (statsManager ? GetStats).mapTo[List[StatsAction]].map { stats =>
        Ok(Json.toJson(stats))
      }
    }
  }

  def getStatisticTemplate(): Action[AnyContent] = Action {
    Ok(statsActionTemp())
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    logger.debug(s"Upload file is started...")
    val result = request.body
      .file("file")
      .map { picture =>
        logger.debug(s"picture: ${picture.filename}")
        val body = request.body.asFormUrlEncoded
        body.get("id").flatMap(_.headOption) match {
          case Some(customerId) =>
            // need to create folder "patients_results" out of the project
            val time_stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())
            val analysisFileName = customerId + "_" + time_stamp + ".jpg"
            logger.debug(s"Path: $tempFilesPath}")
            Try {
              picture.ref.copyTo(Paths.get(tempFilesPath + "/" + analysisFileName), replace = true)
            }.recover {
              case e: Throwable =>
                logger.error("Error while parsing tempFilePath", e)
            }
            (for {
              _ <- EitherT((patientManager ? AddAnalysisResult(customerId, analysisFileName)).mapTo[Either[String, String]])
              _ <- EitherT((patientManager ? SendSmsToCustomer(customerId)).mapTo[Either[String, String]])
            } yield {
              val statsAction = StatsAction(LocalDateTime.now, request.host, action = "doc_upload", request.headers.get("Remote-Address").get, request.headers.get("User-Agent").get)
              statsManager ! AddStatsAction(statsAction)
              "File is uploaded"
            }).recover {
              case error: Any =>
                logger.error("Error while uploading image", error)
                "Something went wrong"
            }.value
          case None =>
            logger.error("Customer ID not found")
            Future.successful(Left("Customer ID not found"))
        }
      }.getOrElse {
      logger.debug(s"No file to upload")
      Future.successful(Left("Missing file"))
    }
    result.map {
      case Right(redirectWithSuccess) =>
        logger.debug("File successfully uploaded")
        Ok(redirectWithSuccess)
      case Left(error) =>
        logger.error(s"Something bad happened", error)
       BadRequest(error)
    }.recover {
      case e: Throwable =>
        logger.error(s"Unexpected error happened", e)
        BadRequest("Unexpected error happened")
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
                case "doc" => Redirect("/doc").addingToSession(LoginKey -> DoctorLoginKey)
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

  def stubSmsRequest: Action[AnyContent] = Action { implicit request =>
    val body = request.body.asFormUrlEncoded
    logger.debug(s"Stub SMS Request: $body")
    Ok("""[{"recipient":"998994461230","text":"Tahlil natijasini kuyidagi xavola orqali olishingiz mumkin:http://localhost:9000/analysis-result/H-864","date_received":"2021-01-14T07:56:29.276Z","client_id":"5ffd479e88ab87825645b4e7","request_id":430349076,"message_id":370341299,"ip":"195.158.8.42","_id":"5ffff92d8c857d034a22c5b9"}]""")
//    Ok("""[{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101},{"error":1,"text":"Incorrect Login or Password","error_no":101}]""")
  }

  def stubSmsDeliveryStatus: Action[AnyContent] = Action { implicit request =>
    val body = request.body.asFormUrlEncoded
    logger.debug(s"Stub SMS Delivery Status: $body")
    Ok("""{"messages":[{"message-id":735211340,"channel":"SMS","status":"Delivered","status-date":"2021-01-14 14:34:23"}]}""")
  }

  private def generateCustomerId = randomStr(1).toUpperCase + "-" + getRandomDigits(3)

  private def generateLogin = randomStr(1).toUpperCase + "-" + getRandomDigit(3)

  private def generatePassword = getRandomPassword(7)
}