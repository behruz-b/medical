package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._
import protocols.Authentication.AppRole._
import protocols.PatientProtocol._
import protocols.UserProtocol.{CheckUserByLoginAndCreate, GetRoles, Roles, User}
import views.html._
import views.html.statistic._

import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time._
import java.util.Date
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
  val tempFilesPath: String = configuration.get[String]("analysis_folder")
  val tempFolderPath: String = configuration.get[String]("temp_folder")
  val adminLogin: String = configuration.get[String]("admin.login")
  val adminPassword: String = configuration.get[String]("admin.password")

  def index(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(RegRole, language) {
      Ok(indexTemplate(language))
    }
  }

  def authByDashboard(role: String, lang: String = "uz")(result: => Result)
                     (implicit request: RequestHeader): Result = {
    val res = authByRole(role)(result)
    if (res.header.status == UNAUTHORIZED) {
      Ok(loginPage(lang))
    } else {
      result
    }
  }

  def admin(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(AdminRole, language) {
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
          BadRequest("So'ralgan bemor tekshirinuv natijasi topilmadi!")
        }
      case Left(e) => BadRequest(e)
    }.recover {
      case e =>
        logger.error("Error while getting patient", e)
        BadRequest("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!")
    }
  }

  def createDoctor: Action[DoctorForm] = Action.async(parse.json[DoctorForm]) { implicit request =>
    authByRole(AdminRole) {
      val body = request.body
      val phone = "998" + clearPhone(body.phone)
      val company_code = request.host
      val user = User(LocalDateTime.now, body.firstName, body.lastName, phone, body.role, company_code, body.login, generatePassword)
      (userManager ? CheckUserByLoginAndCreate(user)).mapTo[Either[String, String]].map {
        case Right(_) =>
          Ok(Json.toJson(user))
        case Left(error) =>
          BadRequest(error)
      }.recover {
        case e: Throwable =>
          logger.error("Error while creating doctor", e)
          BadRequest("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!")
      }
    }
  }

  def createPatient: Action[PatientForm] = Action.async(parse.json[PatientForm]) { implicit request =>
    authByRole(RegRole) {
      val body = request.body
      val prefixPhone = "998"
      val company_code = request.host
      val docPhoneWithPrefix = body.docPhone.map(p => prefixPhone + p)
      val phoneWithPrefix = prefixPhone + body.phone
      val login = (body.firstName.head.toString + body.lastName).toLowerCase() + getRandomDigit(3)
      val patient = Patient(LocalDateTime.now, body.firstName, body.lastName, phoneWithPrefix, generateCustomerId,
        company_code, login, generatePassword, body.address, body.dateOfBirth, body.analyseType, body.docFullName, docPhoneWithPrefix)
      (patientManager ? CreatePatient(patient)).mapTo[Either[String, String]].map {
        case Right(_) =>
          val stats = StatsAction(LocalDateTime.now, request.host, action = "reg_submit", request.headers.get("Remote-Address").get,
            request.session.get(createSessionKey(request.host)).getOrElse(createSessionKey(request.host)), request.headers.get("User-Agent").get)
          statsManager ! AddStatsAction(stats)
          Ok(Json.toJson(patient.customer_id))
        case Left(e) => BadRequest(e)
      }.recover {
        case e: Throwable =>
          logger.error("Error while creating patient", e)
          BadRequest("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!")
      }
    }
  }

  def addAnalysisResult(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(DoctorRole, language) {
      Ok(addAnalysisResultPageTemp(language))
    }
  }

  def getPatients: Action[AnyContent] = Action.async { implicit request =>
    authByRole(DoctorRole) {
      (patientManager ? GetPatients).mapTo[List[Patient]].map { patients =>
        logger.debug(s"patients: $patients")
        Ok(Json.toJson(patients))
      }
    }
  }

  def getPatientsTemplate(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(PatientRole, language) {
      Ok(getPatientsTemp(language))
    }
  }

  def getStats: Action[AnyContent] = Action.async { implicit request =>
    authByRole(StatsRole) {
      (statsManager ? GetStats).mapTo[List[StatsAction]].map { stats =>
        Ok(Json.toJson(stats))
      }
    }
  }

  def getStatisticTemplate(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(StatsRole, language) {
      Ok(statsActionTemp(language))
    }
  }

  def getAnalysisType: Action[AnyContent] = Action { implicit request =>
    authByRole(RegRole) {
      Ok(Json.toJson(analysisType))
    }
  }

  def getRoleTypes: Action[AnyContent] = Action.async { implicit request =>
    authByRole(AdminRole) {
      (userManager ? GetRoles).mapTo[List[Roles]].map { results =>
        Ok(Json.toJson(results))
      }
    }
  }

  def uploadAnalysisResult: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    authByRole(DoctorRole) {
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
                val statsAction = StatsAction(LocalDateTime.now, request.host, "doc_upload", request.headers.get("Remote-Address").get, request.session.get(createSessionKey(request.host)).getOrElse(createSessionKey(request.host)), request.headers.get("User-Agent").get)
                statsManager ! AddStatsAction(statsAction)
                statsManager ! AddStatsAction(statsAction.copy(action = "doc_send_sms"))
                "File is uploaded"
              }).value.recover { e =>
                logger.error("Unexpected error happened", e)
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
        case Right(res) =>
          logger.debug("File successfully uploaded")
          Ok(res)
        case Left(error) =>
          logger.error("Something bad happened", error)
          BadRequest(error)
      }.recover {
        case e: Throwable =>
          logger.error("Unexpected error happened", e)
          BadRequest("Unexpected error happened")
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

  private def generatePassword = getRandomPassword(7)

}