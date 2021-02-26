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
import protocols.AppProtocol.Paging.{PageReq, PageRes}
import protocols.Authentication.LoginSessionKey
import protocols.PatientProtocol._
import protocols.UserProtocol.{CheckUserByLoginAndCreate, SendSmsToDoctor, User}
import views.html._

import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time._
import java.util.Date
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class PatientController @Inject()(val controllerComponents: ControllerComponents,
                                  val dashboardTemp: views.html.dashboard.dashboard,
                                  patientsDocTemplate: views.html.patientsDoc.patientsDoc,
                                  loginPage: views.html.admin.login,
                                  configuration: Configuration,
                                  addAnalysisResultPageTemp: addAnalysisResult.addAnalysisResult,
                                  getPatientsTemp: patients.patientsTable,
                                  @Named("patient-manager") val patientManager: ActorRef,
                                  @Named("user-manager") val userManager: ActorRef,
                                  @Named("stats-manager") val statsManager: ActorRef,
                                  @Named("patients-doc-manager") val patientsDocManager: ActorRef)
                                 (implicit val webJarsUtil: WebJarsUtil, implicit val ec: ExecutionContext)
  extends BaseController
    with CommonMethods
    with Auth
    with CustomErrorHandler {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  val tempFilesPath: String = configuration.get[String]("analysis_folder")
  val tempFolderPath: String = configuration.get[String]("temp_folder")
  val adminLogin: String = configuration.get[String]("admin.login")
  val adminPassword: String = configuration.get[String]("admin.password")

  private def isAuthorized(implicit request: RequestHeader): Boolean = request.session.get(LoginSessionKey).isDefined

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

  def getAnalysisResultsAndSafeStats(customerId: String): Action[AnyContent] = Action.async { implicit request =>
    (patientManager ? GetAnalysisResultsByCustomerId(customerId)).mapTo[Either[String, PatientAnalysisResult]].map {
      case Right(patient) =>
        val stats = StatsAction(LocalDateTime.now, request.host, "result_sms_click", getRemoteAddress,
          login = patient.customerId, getUserAgent)
        statsManager ! AddStatsAction(stats)
        val patientStats = AddSmsLinkClick(customerId = patient.customerId, smsLinkClick = "click")
        patientManager ! patientStats
        Ok.sendFile(new java.io.File(tempFilesPath + "/" + patient.analysisFileName))
      case Left(e) =>
        BadRequest(e)
    }.recover(handleErrorWithStatus("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!",
      "Error while getting patient"))
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
      }.recover(handleErrorWithStatus("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!", "Error while creating doctor"))
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
      }.recover(handleErrorWithStatus("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!",
        "Error while creating doctor"))
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
      getUniqueCustomerId(patient)
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

  def getPatients(page: Int, pageSize: Int): Action[PatientsReport] = Action.async(parse.json[PatientsReport]) { implicit request =>
    authByRole(isManager || isDoctor) {
      val pageReq = PageReq(page = page, size = pageSize)
      val body = request.body
      logger.debug(s"startDate: ${body.startDate}")
      logger.debug(s"endDate: ${body.endDate}")
      (patientManager ? GetPatients(body.analyseType, body.startDate, body.endDate, pageReq))
        .mapTo[Either[String, PageRes[Patient]]].map {
        case Right(p) => Ok(Json.toJson(p))
        case Left(r) => BadRequest(r)
      }.recover {
        case e =>
          logger.error("Error occurred", e)
          BadRequest("Error while requesting Patients")
      }
    }
  }

  def getPatientsTemplate(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isManager || isDoctor, language) {
      Ok(getPatientsTemp(isAuthorized, isManager, isAdmin, language))
    }
  }

  def getPatientsDoc: Action[AnyContent] = Action.async { implicit request =>
    authByRole(isRegister || isManager) {
      (patientsDocManager ? GetPatientsDoc).mapTo[List[GetPatientsDocById]].map { patientsDoc =>
        Ok(Json.toJson(patientsDoc))
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
                _ <- EitherT((patientManager ? PatientAnalysisResult(analysisFileName, LocalDateTime.now, customerId)).mapTo[Either[String, String]])
                _ <- EitherT((patientManager ? SendSmsToCustomer(customerId)).mapTo[Either[String, String]])
              } yield {
                val statsAction = StatsAction(LocalDateTime.now, request.host, "doc_upload", getRemoteAddress, getUserLogin, getUserAgent)
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

  private def getUniqueCustomerId(patient: Patient, attempts: Int = 1): Future[Result] = {
    if (attempts < 5) {
      (patientManager ? CheckCustomerId(patient.customer_id)).mapTo[Either[String, Patient]].flatMap {
        case Left(_) =>
          createPatientInDB(patient)
        case Right(_) =>
          val withNewCustomerId = patient.copy(customer_id = generateCustomerId)
          getUniqueCustomerId(withNewCustomerId, attempts + 1)
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

  def searchByPatientName(firstname: String): Action[AnyContent] = Action.async { _ =>
    (patientManager ? SearchByPatientName(firstname)).mapTo[Either[String, List[Patient]]].map {
      case Right(patients) =>
        Ok(Json.toJson(patients))
      case Left(e) => BadRequest(e)
    }.recover {
      case e =>
        logger.error("Error while getting patient", e)
        BadRequest("Xatolik yuz berdi iltimos qayta harakat qilib ko'ring!")
    }
  }

  private def generateCustomerId = randomStr(1).toUpperCase + "-" + getRandomDigits(3)

  private def generatePassword = getRandomPassword(7)

}