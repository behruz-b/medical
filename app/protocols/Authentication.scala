package protocols

import cats.implicits.catsSyntaxOptionId
import controllers.routes
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc.Call

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Authentication {
  val Company = Vector("localhost:9000", "smart-medical.uz")

  case class LoginFormWithClientCode(
    login: String = "",
    password: String = "",
  )
  def createSessionKey: String => String = _ + ".session.key"

  implicit val loginPlayFormWithClientCode: Form[LoginFormWithClientCode] = Form {
    mapping(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText,
//      "clientCode" -> nonEmptyText
    )(LoginFormWithClientCode.apply)(LoginFormWithClientCode.unapply)
  }
  case class Login(redirectUrl: Call,
                   companyCode: String,
                   sessionKey: String,
                   sessionDuration: Option[FiniteDuration] = 60.minutes.some)

  val loginPatters: Map[String, Login] = Company.flatMap { domain =>
    Vector(
      Login(routes.HomeController.index(), domain, createSessionKey(domain)),
      Login(routes.HomeController.admin(), domain, createSessionKey(domain)),
      Login(routes.HomeController.addAnalysisResult(), domain, createSessionKey(domain))
    )
  }.map( l => l.redirectUrl.url -> l).toMap

}
