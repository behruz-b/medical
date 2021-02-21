package protocols

import cats.implicits.catsSyntaxOptionId
import controllers.{CommonMethods, routes}
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc.Call

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Authentication extends CommonMethods {

  object AppRole {
    val DoctorRole = "doctor.role"
    val RegRole = "register.role"
    val AdminRole = "admin.role"
    val ManagerRole = "manager.role"
  }

  import AppRole._

  val LoginSessionKey = "login.session.key"

  case class LoginFormWithClientCode(
                                      login: String = "",
                                      password: String = "",
                                    )

  implicit val loginPlayFormWithClientCode: Form[LoginFormWithClientCode] = Form {
    mapping(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText,
    )(LoginFormWithClientCode.apply)(LoginFormWithClientCode.unapply)
  }

  case class SessionAttr(sessionKey: String, roleSessionKey: Set[String])

  case class Login(rootPath: String,
                   redirectUrl: Call,
                   sessionAttr: SessionAttr,
                   sessionDuration: Option[FiniteDuration] = 60.minutes.some)

  val loginPatterns: Map[String, Login] = Vector(
      Login("/reg/", routes.HomeController.registerPage(), createSessionAttr(Set(RegRole, ManagerRole, AdminRole))),
      Login("/admin/", routes.HomeController.admin(), createSessionAttr(Set(AdminRole))),
      Login("/doc/", routes.PatientController.addAnalysisResult(), createSessionAttr(Set(DoctorRole, ManagerRole, AdminRole))),
      Login("/patient/", routes.PatientController.getPatientsTemplate(), createSessionAttr(Set(ManagerRole, AdminRole))),
      Login("/stats/", routes.HomeController.getStatisticTemplate(), createSessionAttr(Set(AdminRole))),
      Login("/doctors/", routes.PatientController.patientsDocPage(), createSessionAttr(Set(RegRole, ManagerRole, AdminRole))),
      Login("/change-password/", routes.HomeController.changePass(), createSessionAttr(Set(RegRole, DoctorRole, ManagerRole, AdminRole)))
    ).map(l => l.rootPath -> l).toMap
}
