package controllers

import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time._
import java.util.Date

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import javax.inject._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import protocols.Authentication.{LoginSessionKey, LoginWithSession}
import protocols.PatientProtocol._
import protocols.UserProtocol.{ChangePassword, CheckUserByLoginAndCreate, GetRoles, Roles, SendSmsToDoctor, User}
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
                               patientsDocTemplate: views.html.patientsDoc.patientsDoc,
                               adminTemplate: views.html.admin.adminPage,
                               passTemplate: views.html.changePassword.changePassword,
                               loginPage: views.html.admin.login,
                               configuration: Configuration,
                               addAnalysisResultPageTemp: addAnalysisResult.addAnalysisResult,
                               statsActionTemp: statisticTemplete,
                               getPatientsTemp: patients.patientsTable,
                               @Named("patient-manager") val patientManager: ActorRef,
                               @Named("user-manager") val userManager: ActorRef,
                               @Named("stats-manager") val statsManager: ActorRef,
                               @Named("patients-doc-manager") val patientsDocManager: ActorRef)
                              (implicit val webJarsUtil: WebJarsUtil, implicit val ec: ExecutionContext)
  extends BaseController with CommonMethods with Auth {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  val tempFilesPath: String = configuration.get[String]("analysis_folder")
  val tempFolderPath: String = configuration.get[String]("temp_folder")
  val adminLogin: String = configuration.get[String]("admin.login")
  val adminPassword: String = configuration.get[String]("admin.password")

  private def isAuthorized(implicit request: RequestHeader): Boolean = request.session.get(LoginSessionKey).isDefined

  def index(language: String): Action[AnyContent] = Action { implicit request =>
    Ok(indexTemplate(isAuthorized, isManager, isAdmin, language))
  }

  def dashboard(language: String): Action[AnyContent] = Action { implicit request =>
    Ok(dashboardTemp(isAuthorized, isManager, isAdmin, language))
  }

  def changePass(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isRegister || isAdmin || isDoctor || isManager) {
      Ok(passTemplate(isAuthorized, isManager, isAdmin, language))
    }
  }

  def changePassword: Action[ChangePassword] = Action.async(parse.json[ChangePassword]) { implicit request =>
    authByRole(isDoctor || isRegister || isAdmin) {
      val body = request.body
      (userManager ? ChangePassword(body.login,body.newPass)).mapTo[Either[String, String]].map {
        case Right(_) =>
          Ok(Json.toJson("Successfully updated"))
        case Left(error) =>
          BadRequest(error)
      }.recover {
        case e: Throwable =>
          logger.error("Error while creating doctor", e)
          BadRequest("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!")
      }
    }
  }

  def registerPage(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isRegister || isManager, language) {
      Ok(regTemplate(isAuthorized, isManager, isAdmin, language))
    }
  }

  def patientsDocPage(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isRegister || isManager, language) {
      Ok(patientsDocTemplate(isAuthorized, isManager, isAdmin, language))
    }
  }

  def authByDashboard(hasAccess: Boolean, lang: String = "uz")(result: => Result)
                     (implicit request: RequestHeader): Result = {
    val res = authByRole(hasAccess)(result)
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

  def analysisResult(customerId: String): Action[AnyContent] = Action.async {
    (patientManager ? GetPatientByCustomerId(customerId)).mapTo[Either[String, Patient]].map {
      case Right(patient) =>
        if (patient.analysis_image_name.isDefined) {
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

  def analysisResultWithStats(customerId: String): Action[AnyContent] = Action.async { implicit request =>
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

  def addPatientsDoc: Action[PatientsDocForm] = Action.async(parse.json[PatientsDocForm]) { implicit request =>
    authByRole(isRegister || isManager) {
      val body = request.body
      val phone = "998" + clearPhone(body.phone)
      val patientsDoc = PatientsDoc(body.fullName, phone)
      (patientsDocManager ? AddPatientsDoc(patientsDoc)).mapTo[Either[String, String]].map {
        case Right(_) =>
          Ok(Json.toJson("Successfully added"))
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
      val phoneWithPrefix = prefixPhone + body.phone
      val login = (body.firstName.head.toString + body.lastName).toLowerCase() + getRandomDigit(3)
      val patient = Patient(LocalDateTime.now, body.firstName, body.lastName, phoneWithPrefix, generateCustomerId,
        body.companyCode, login, generatePassword, body.address, body.dateOfBirth, body.analyseType, body.analyseGroup,
        body.docFullName, body.docPhone, docId = body.docId)
      getUniqueCustomerId(1, patient)
//      val stats = StatsAction(LocalDateTime.now, body.companyCode, action = "reg_submit", request.headers.get("Remote-Address").get,
//        request.session.get(LoginWithSession).getOrElse(LoginWithSession), request.headers.get("User-Agent").get)
//      statsManager ! AddStatsAction(stats)
    }
  }

  def addAnalysisResult(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isDoctor || isManager, language) {
      Ok(addAnalysisResultPageTemp(isAuthorized, isManager, isAdmin, language))
    }
  }

  def getPatients: Action[JsValue] = Action.async(parse.json) { implicit request =>
    authByRole(isAdmin || isManager || isDoctor) {
      val analyseType = (request.body \ "analyseType").asOpt[String].flatMap(v => if (v.trim.isBlank) None else v.some)
      (patientManager ? GetPatientsForm(analyseType)).mapTo[Either[String, List[Patient]]].map {
        case Right(p) => Ok(Json.toJson(p))
        case Left(r) => BadRequest(r.toString)
      }.recover {
        case e =>
          logger.error("Error occurred", e)
          BadRequest("Error while requesting Patients")
      }
    }
  }

  def getPatientsTemplate(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isAdmin || isManager || isDoctor, language) {
      Ok(getPatientsTemp(isAuthorized, isManager, isAdmin, language))
    }
  }

  def getStats: Action[AnyContent] = Action.async { implicit request =>
    authByRole(isAdmin) {
      (statsManager ? GetStats).mapTo[List[StatsAction]].map { stats =>
        Ok(Json.toJson(stats))
      }
    }
  }

  def getPatientsDoc: Action[AnyContent] = Action.async { implicit request =>
    authByRole(isRegister || isManager) {
      (patientsDocManager ? GetPatientsDoc).mapTo[List[GetPatientsDocById]].map { patientsDoc =>
        Ok(Json.toJson(patientsDoc))
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

  def getLabType: Action[AnyContent] = Action { implicit request =>
    authByRole(isRegister || isManager) {
      Ok(Json.toJson(laboratoryType))
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
                val statsAction = StatsAction(LocalDateTime.now, request.host, "doc_upload", request.headers.get("Remote-Address").get, request.session.get(LoginWithSession).getOrElse(LoginWithSession), request.headers.get("User-Agent").get)
                statsManager ! AddStatsAction(statsAction)
                (userManager ? SendSmsToDoctor(customerId)).mapTo[Either[String, String]].recover {
                  case e: Throwable =>
                    logger.error("Unexpected error happened", e)
                    BadRequest("Something went wrong")
                }
                "File is uploaded"
              }).value.recover {
                case e =>
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

  private def getUniqueCustomerId(attempts: Int, patient: Patient): Future[Result] = {
    if (attempts < 5) {
      (patientManager ? CheckCustomerId(patient.customer_id)).mapTo[Either[String, Patient]].flatMap {
        case Left(_) =>
          createPatientInDB(patient)
        case Right(_) =>
          val withNewCustomerId = patient.copy(customer_id = generateCustomerId)
          getUniqueCustomerId(attempts + 1, withNewCustomerId)
      }
    } else {
      logger.error("Couldn't generate unique customerId")
      Future.successful(BadRequest("Couldn't generate customerId"))
    }
  }

  private def createPatientInDB(patient: Patient): Future[Result] = {
    (patientManager ? CreatePatient(patient)).mapTo[Either[String, String]].flatMap {
      case Right(_) =>
        (patientManager ? SendIdToPatientViaSms(patient.customer_id)).mapTo[Either[String, String]].map { _ =>
          Ok(Json.toJson(patient.customer_id))
        }.recover {
          case e =>
            logger.error("Unexpected error happened", e)
            BadRequest("Something went wrong")
        }
      case Left(e) => Future.successful(BadRequest(e))
    }.recover {
      case e: Throwable =>
        logger.error("Error while creating patient", e)
        BadRequest("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!")
    }
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