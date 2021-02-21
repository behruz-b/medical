package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import protocols.Authentication.LoginSessionKey
import protocols.PatientProtocol._
import protocols.UserProtocol.{ChangePassword, GetRoles, Roles}
import views.html._
import views.html.statistic._

import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val dashboardTemp: views.html.dashboard.dashboard,
                               indexTemplate: views.html.index,
                               regTemplate: views.html.register.register,
                               adminTemplate: views.html.admin.adminPage,
                               passTemplate: views.html.changePassword.changePassword,
                               loginPage: views.html.admin.login,
                               configuration: Configuration,
                               statsActionTemp: statisticTemplete,
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

  def index(language: String): Action[AnyContent] = Action { implicit request =>
    Ok(indexTemplate(isAuthorized, isManager, isAdmin, language))
  }

  def dashboard(language: String): Action[AnyContent] = Action { implicit request =>
    Ok(dashboardTemp(isAuthorized, isManager, isAdmin, language))
  }

  def changePass(language: String): Action[AnyContent] = Action { implicit request =>
    authByDashboard(isRegister || isDoctor || isManager) {
      Ok(passTemplate(isAuthorized, isManager, isAdmin, language))
    }
  }

  def changePassword: Action[JsValue] = Action.async(parse.json) { implicit request =>
    authByRole(isDoctor || isRegister || isManager) {
      val newPass = (request.body \ "newPass").as[String]
      (userManager ? ChangePassword(getUserLogin, newPass)).mapTo[Either[String, String]].map {
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

  def getStats: Action[AnyContent] = Action.async { implicit request =>
    authByRole(isAdmin) {
      (statsManager ? GetStats).mapTo[List[StatsAction]].map { stats =>
        Ok(Json.toJson(stats.reverse))
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
}