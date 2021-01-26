package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import protocols.PatientProtocol._
import protocols.UserProtocol.{User, checkUserByLoginAndCreate}
import views.html._
import views.html.statistic._

import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time._
import java.util.Date
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               indexTemplate: views.html.index,
                               adminTemplate: views.html.admin.adminPage,
                               loginPage: views.html.admin.login,
                               configuration: Configuration,
                               addAnalysisResultPageTemp: addAnalysisResult.addAnalysisResult,
                               statsActionTemp: statisticTemplete,
                               getPatientsTemp: patients.patientsTable,
                               @Named("patient-manager") val patientManager: ActorRef,
                               @Named("user-manager") val userManager: ActorRef,
                               @Named("stats-manager") val statsManager: ActorRef)
                              (implicit val webJarsUtil: WebJarsUtil, implicit val ec: ExecutionContext)
  extends BaseController with CommonMethods with Auth {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  val LoginKey = "login_session_key"
  val DoctorLoginKey = "doctor.role"
  val RegLoginKey = "register.role"
  val AdminLoginKey = "admin.role"
  val SessionLogin = "login_session"
  val StatsRole = "stats.role"
  val PatientsAdmin = "patients_admin"
  val tempFilesPath: String = configuration.get[String]("analysis_folder")
  val tempFolderPath: String = configuration.get[String]("temp_folder")
  val adminLogin: String = configuration.get[String]("admin.login")
  val adminPassword: String = configuration.get[String]("admin.password")

  def index(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(RegLoginKey, language) {
      Ok(indexTemplate(language))
    }
  }

  def authByDashboard(role: String, lang: String = "uz")(result: => Result)
                     (implicit request: RequestHeader): Result = {
    val res = authByRole(role)(result)
    if (res.header.status == UNAUTHORIZED) {
      Ok(loginPage(lang)).flashing("error" -> "You haven't got right role to see page")
    } else {
      result
    }
  }

  def admin(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(AdminLoginKey, language) {
      Ok(adminTemplate(language))
    }
  }

  def analysisResult(customerId: String): Action[AnyContent] = Action.async { implicit request =>
    (patientManager ? GetPatientByCustomerId(customerId)).mapTo[Either[String, Patient]].map {
      case Right(patient) =>
        if (patient.analysis_image_name.isDefined) {
          val stats = StatsAction(LocalDateTime.now, request.host, action = "result_sms_click", request.headers.get("Remote-Address").get,
            login = patient.customer_id, request.headers.get("User-Agent").get)
          statsManager ! AddStatsAction(stats)
          Ok.sendFile(new java.io.File(tempFilesPath + "/" + patient.analysis_image_name.get))
        } else {
          logger.error("Error while getting analysis file name")
          BadRequest("Error")
        }
      case Left(e) =>
        BadRequest(e)
    }.recover {
      case e =>
        logger.error("Error while getting patient", e)
        BadRequest("Error")
    }
  }

  def createDoctor: Action[JsValue] = Action.async(parse.json) { implicit request =>
    authByRole(DoctorLoginKey) {
      Try {
        val firstName = (request.body \ "firstName").as[String]
        val lastName = (request.body \ "lastName").as[String]
        val phone = (request.body \ "phone").as[String]
        val role = (request.body \ "role").as[String]
        val prefixPhone = "998"
        val company_code = request.host
        val login = (request.body \ "login").as[String]
        val user = User(LocalDateTime.now, firstName, lastName, prefixPhone + phone, role,
          company_code, login, generatePassword)
        (userManager ? checkUserByLoginAndCreate(user)).mapTo[Either[String, String]].map {
          case Right(_) =>
            Ok(Json.toJson(user))
          case Left(error) =>
            BadRequest(error)
        }.recover {
          case e: Throwable =>
            logger.error("Error while creating doctor", e)
            Redirect("/admin").flashing("error" -> "Error")
        }
      } match {
        case Success(res) => res
        case Failure(exception) =>
          logger.error("Error occurred while create doctor. Error:", exception)
          Future.successful(Redirect("/admin").flashing("error" -> "Ro'yhatdan o'tishda xatolik yuz berdi. Iltimos qaytadan harakat qilib ko'ring!"))
      }
    }
  }

  def createPatient: Action[JsValue] = Action.async(parse.json) { implicit request =>
    Try {
      val firstName = (request.body \ "firstName").as[String]
      val lastName = (request.body \ "lastName").as[String]
      val phone = (request.body \ "phone").as[String]
      val prefixPhone = "998"
      val company_code = request.host
      val dateOfBirth = (request.body \ "date").as[String]
      val dateCheck = if (dateOfBirth.length == 8) {
        val yearOfBirth = dateOfBirth.split("/").reverse.head
        val fillYear = if (0 <= yearOfBirth.toInt && yearOfBirth.toInt <= 21) {
          "20" + yearOfBirth
        } else {
          "19" + yearOfBirth
        }
        val fillDate = fillYear + "/" + dateOfBirth.split("/").reverse.tail.mkString("/")
        val dateOfBirthday = fillDate.split("/").reverse.mkString("/")
        dateOfBirthday
      } else {
        val dateOfBirthday = (request.body \ "date").as[String]
        dateOfBirthday
      }
      val address = (request.body \ "address").as[String]
      val analyseType = (request.body \ "analysisType").as[String]
      val docFullName = (request.body \ "docFullName").asOpt[String]
      val docPhone = (request.body \ "docPhone").asOpt[String]
      val docPhoneWithPrefix = docPhone.map(p => prefixPhone + p)
      val login = (firstName.head.toString + lastName).toLowerCase() + getRandomDigit(3)
      val patient = Patient(LocalDateTime.now, firstName, lastName, prefixPhone + phone, generateCustomerId,
        company_code, login, generatePassword, address, parseDate(dateCheck), analyseType, docFullName, docPhoneWithPrefix)
      (patientManager ? CreatePatient(patient)).mapTo[Either[String, String]].map {
        case Right(_) =>
          val stats = StatsAction(LocalDateTime.now, request.host, action = "reg_submit", request.headers.get("Remote-Address").get,
            request.session.get(SessionLogin).getOrElse(SessionLogin), request.headers.get("User-Agent").get)
          statsManager ! AddStatsAction(stats)
          Ok(Json.toJson(patient.customer_id))
        case Left(e) =>
          logger.debug(s"ERROR")
          Redirect("/reg").flashing("error" -> e)
      }.recover {
        case e: Any =>
          logger.error("Error while creating patient", e)
          Redirect("/reg").flashing("error" -> "Error")
      }
    } match {
      case Success(res) => res
      case Failure(exception) =>
        logger.error("Error occurred while create patient. Error:", exception)
        Future.successful(Redirect("/reg").flashing("error" -> "Ro'yhatdan o'tishda xatolik yuz berdi. Iltimos qaytadan harakat qilib ko'ring!"))
    }
  }

  def addAnalysisResult(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(DoctorLoginKey, language) {
      Ok(addAnalysisResultPageTemp(language))
    }
  }

  def getPatients: Action[AnyContent] = Action.async { implicit request =>
    authByRole(DoctorLoginKey) {
      (patientManager ? GetPatients).mapTo[List[Patient]].map { patients =>
        logger.debug(s"patients: $patients")
        Ok(Json.toJson(patients))
      }
    }
  }

  def getPatientsTemplate: Action[AnyContent] = Action { implicit request =>
    authByDashboard(DoctorLoginKey) {
      Ok(getPatientsTemp())
    }
  }

  def getStats: Action[AnyContent] = Action.async { implicit request =>
    authByRole(StatsRole) {
      (statsManager ? GetStats).mapTo[List[StatsAction]].map { stats =>
        Ok(Json.toJson(stats))
      }
    }
  }

  def getStatisticTemplate: Action[AnyContent] = Action { implicit request =>
    authByDashboard(StatsRole) {
      Ok(statsActionTemp())
    }
  }

  def getAnalysisType: Action[AnyContent] = Action { implicit request =>
    authByRole(RegLoginKey) {
      Ok(Json.toJson(analysisType))
    }
  }

  def getRoleTypes: Action[AnyContent] = Action { implicit request =>
    authByRole(AdminLoginKey) {
      Ok(Json.toJson(roleTypes))
    }
  }

  def uploadAnalysisResult: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    val result = request.body
      .file("file")
      .map { picture =>
        val body = request.body.asFormUrlEncoded
        body.get("id").flatMap(_.headOption) match {
          case Some(customerId) =>
            // need to create folder "patients_results" out of the project
            val time_stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date())
            val analysisFileName = customerId + "_" + time_stamp + ".jpg"
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
              val statsAction = StatsAction(LocalDateTime.now, request.host, "doc_upload", request.headers.get("Remote-Address").get, request.session.get(SessionLogin).getOrElse(SessionLogin), request.headers.get("User-Agent").get)
              statsManager ! AddStatsAction(statsAction)
              statsManager ! AddStatsAction(statsAction.copy(action = "doc_send_sms"))
              "File is uploaded"
            }).value.recover { e =>
              logger.error(s"Unexpected error happened", e)
              Left("Something went wrong")
            }
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
        Redirect("/doc").flashing("error" -> error)
    }.recover {
      case e: Throwable =>
        logger.error(s"Unexpected error happened", e)
        Redirect("/doc").flashing("error" -> "Unexpected error happened")
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

  private def generatePassword = getRandomPassword(7)

}