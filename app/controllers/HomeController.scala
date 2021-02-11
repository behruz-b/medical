package controllers

import akka.actor.{ActorRef, ActorSelection, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import com.typesafe.config.Config
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc._
import protocols.AppProtocol.NotifyMessage
import protocols.Authentication.LoginSessionKey
import protocols.PatientProtocol._
import protocols.UserProtocol.{CheckUserByLoginAndCreate, GetRoles, Roles, SendSmsToDoctor, User}
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
                               val dashboardTemp: views.html.dashboard.dashboard,
                               indexTemplate: views.html.index,
                               regTemplate: views.html.register.register,
                               adminTemplate: views.html.admin.adminPage,
                               loginPage: views.html.admin.login,
                               configuration: Configuration,
                               addAnalysisResultPageTemp: addAnalysisResult.addAnalysisResult,
                               statsActionTemp: statisticTemplete,
                               getPatientsTemp: patients.patientsTable,
                               @Named("patient-manager") val patientManager: ActorRef,
                               //                               @Named("monitoring-notifier") val monitoring: ActorRef,
                               @Named("user-manager") val userManager: ActorRef,
                               @Named("stats-manager") val statsManager: ActorRef)
                              (implicit val webJarsUtil: WebJarsUtil, implicit val ec: ExecutionContext)
  extends BaseController with CommonMethods with Auth {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  val tempFilesPath: String = configuration.get[String]("analysis_folder")
  val tempFolderPath: String = configuration.get[String]("temp_folder")
  val adminLogin: String = configuration.get[String]("admin.login")
  val adminPassword: String = configuration.get[String]("admin.password")

  val actorConfig: Config = configuration.get[Configuration]("monitoring-actor").underlying
  val monitoringActorSystemPath: String = configuration.get[String]("monitoring-notifier")
  lazy val actorSystem: ActorSystem = ActorSystem("medical", actorConfig)
  lazy val notifierManager: ActorSelection = actorSystem.actorSelection(monitoringActorSystemPath)

  private def isAuthorized(implicit request: RequestHeader): Boolean = request.session.get(LoginSessionKey).isDefined

  def index(language: String): Action[AnyContent] = Action { implicit request =>
    Ok(indexTemplate(isAuthorized, isManager, language))
  }

  def dashboard(language: String): Action[AnyContent] = Action {
    Ok(dashboardTemp(language))
  }

  def registerPage(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isRegister || isManager, language) {
      Ok(regTemplate(isAuthorized, isManager, language))
    }
  }

  def authByDashboard(hasAccess: Boolean, lang: String = "uz")(result: => Result)
                     (implicit request: RequestHeader): Result = {
    val res = authByRole(hasAccess)(result)
        logger.debug(s"monitoring: $notifierManager")
    notifierManager ! NotifyMessage(s"errorText")

    if (res.header.status == UNAUTHORIZED) {
      Ok(loginPage(lang))
    } else {
      result
    }
  }

  def admin(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isAdmin, language) {
      Ok(adminTemplate(isAuthorized, language))
    }
  }

  def analysisResult(customerId: String): Action[AnyContent] = Action.async { implicit request =>
    (patientManager ? GetPatientByCustomerId(customerId)).mapTo[Either[String, Patient]].map {
      case Right(patient) =>
        if (patient.analysis_image_name.isDefined) {
          val stats = StatsAction(LocalDateTime.now, request.host, action = "result_sms_click", request.headers.get("Remote-Address").get,
            login = patient.customer_id, request.headers.get("User-Agent").get)
          statsManager ! AddStatsAction(stats)
          val patientStats = AddSmsLinkClick(customerId = patient.customer_id, smsLinkClick = "click")
          patientManager ! patientStats
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
    authByRole(isAdmin) {
      val body = request.body
      val phone = "998" + clearPhone(body.phone)
      val user = User(LocalDateTime.now, body.firstName, body.lastName, phone, body.role, body.company_code, body.login, generatePassword)
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
    authByRole(isRegister || isManager) {
      val body = request.body
      val prefixPhone = "998"
      val docPhoneWithPrefix = body.docPhone.map(p => prefixPhone + p)
      val phoneWithPrefix = prefixPhone + body.phone
      val login = (body.firstName.head.toString + body.lastName).toLowerCase() + getRandomDigit(3)
      val patient = Patient(LocalDateTime.now, body.firstName, body.lastName, phoneWithPrefix, generateCustomerId,
        body.companyCode, login, generatePassword, body.address, body.dateOfBirth, body.analyseType, body.analyseGroup, body.docFullName, docPhoneWithPrefix)
      (patientManager ? CreatePatient(patient)).mapTo[Either[String, String]].map {
        case Right(_) =>
          val stats = StatsAction(LocalDateTime.now, body.companyCode, action = "reg_submit", request.headers.get("Remote-Address").get,
            request.session.get(LoginSessionKey).getOrElse(LoginSessionKey), request.headers.get("User-Agent").get)
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
    authByDashboard(isDoctor || isManager, language) {
      Ok(addAnalysisResultPageTemp(isAuthorized, isManager, language))
    }
  }

  def getPatients: Action[AnyContent] = Action.async { implicit request =>
    authByRole(isAdmin || isManager || isDoctor) {
      (patientManager ? GetPatients).mapTo[List[Patient]].map { patients =>
        Ok(Json.toJson(patients))
      }
    }
  }

  def getPatientsTemplate(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isAdmin || isManager || isDoctor, language) {
      Ok(getPatientsTemp(isAuthorized, isAdmin, language))
    }
  }

  def getStats: Action[AnyContent] = Action.async { implicit request =>
    authByRole(isAdmin) {
      (statsManager ? GetStats).mapTo[List[StatsAction]].map { stats =>
        Ok(Json.toJson(stats))
      }
    }
  }

  def getStatisticTemplate(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isAdmin, language) {
      Ok(statsActionTemp(isAuthorized, language))
    }
  }

  def getAnalysisType: Action[AnyContent] = Action { implicit request =>
    authByRole(isRegister || isManager) {
      Ok(Json.toJson(analysisType))
    }
  }

  def getMrtType: Action[AnyContent] = Action { implicit request =>
    authByRole(isRegister || isManager) {
      Ok(Json.toJson(mrtType))
    }
  }

  def getMsktType: Action[AnyContent] = Action { implicit request =>
    authByRole(isRegister || isManager) {
      Ok(Json.toJson(msktType))
    }
  }

  def getUziType: Action[AnyContent] = Action { implicit request =>
    authByRole(isRegister || isManager) {
      Ok(Json.toJson(uziType))
    }
  }

  def getRoleTypes: Action[AnyContent] = Action.async { implicit request =>
    authByRole(isAdmin) {
      (userManager ? GetRoles).mapTo[List[Roles]].map { results =>
        Ok(Json.toJson(results))
      }
    }
  }

  def uploadAnalysisResult: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request =>
    authByRole(isDoctor || isManager) {
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
                val statsAction = StatsAction(LocalDateTime.now, request.host, "doc_upload", request.headers.get("Remote-Address").get, request.session.get(LoginSessionKey).getOrElse(LoginSessionKey), request.headers.get("User-Agent").get)
                statsManager ! AddStatsAction(statsAction)
                statsManager ! AddStatsAction(statsAction.copy(action = "doc_send_sms"))
                (userManager ? SendSmsToDoctor(customerId)).mapTo[Either[String, String]].recover { e =>
                  logger.error("Unexpected error happened", e)
                  BadRequest("Something went wrong")
                }
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

  //  def getImage(path: String) = {
  //    val fileBytes = java.nio.file.Files.readAllBytes(Paths.get(tempFilesPath).resolve(patient.analysis_image_name.get))
  //    val directoryPath = new java.io.File("public/images")
  //    directoryPath.mkdirs()
  //    val tempFile = java.io.File.createTempFile("elegant_analysis_", ".jpg", directoryPath)
  //    val fos = new java.io.FileOutputStream(tempFile)
  //    fos.write(fileBytes)
  //  }

  private def generateCustomerId = randomStr(1).toUpperCase + "-" + getRandomDigits(3)

  private def generatePassword = getRandomPassword(7)

}