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
    val StatsRole = "stats.role"
    val PatientRole = "patient.role"
  }
  val LoginSessionKey = "login.session.key"
  import AppRole._

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

  case class SessionAttr(sessionKey: String, roleSessionKey: String)

  case class Login(rootPath: String,
                   redirectUrl: Call,
                   sessionAttr: SessionAttr,
                   sessionDuration: Option[FiniteDuration] = 60.minutes.some)

  val loginPatterns: Map[String, Login] = Vector(
      Login("/reg/", routes.HomeController.index(), createSessionAttr(RegRole)),
      Login("/admin/", routes.HomeController.admin(), createSessionAttr(AdminRole)),
      Login("/doc/", routes.HomeController.addAnalysisResult(), createSessionAttr(DoctorRole)),
      Login("/patient/", routes.HomeController.getPatientsTemplate(), createSessionAttr(PatientRole)),
      Login("/stats/", routes.HomeController.getStatisticTemplate(), createSessionAttr(StatsRole))
    ).map(l => l.rootPath -> l).toMap
}
