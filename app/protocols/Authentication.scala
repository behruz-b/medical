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

  import protocols.Authentication.AppRole._

  val Company = Vector("localhost:9000", "smart-medical.uz")

  case class LoginFormWithClientCode(
                                      login: String = "",
                                      password: String = "",
                                    )

  implicit val loginPlayFormWithClientCode: Form[LoginFormWithClientCode] = Form {
    mapping(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText,
      //      "clientCode" -> nonEmptyText
    )(LoginFormWithClientCode.apply)(LoginFormWithClientCode.unapply)
  }

  case class SessionAttr(sessionKey: String, roleSessionKey: String)

  case class Login(rootPath: String,
                   redirectUrl: Call,
                   companyCode: String,
                   sessionAttr: SessionAttr,
                   sessionDuration: Option[FiniteDuration] = 60.minutes.some)

  val loginPatterns: Map[String, Login] = Company.flatMap { domain =>
    Vector(
      Login("/reg/", routes.HomeController.index(), domain, createSessionAttr(domain, RegRole)),
      Login("/admin/", routes.HomeController.admin(), domain, createSessionAttr(domain, AdminRole)),
      Login("/doc/", routes.HomeController.addAnalysisResult(), domain, createSessionAttr(domain, DoctorRole)),
      Login("/patient/", routes.HomeController.getPatientsTemplate(), domain, createSessionAttr(domain, PatientRole)),
      Login("/stats/", routes.HomeController.getStatisticTemplate(), domain, createSessionAttr(domain, StatsRole))
    )
  }.map(l => l.rootPath -> l).toMap
}
