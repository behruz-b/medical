package protocols

import cats.implicits.catsSyntaxOptionId

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Authentication {
  val Company = Vector("0:0:0:0:0:0:0:1", "smart-medical.uz")

  case class Login(companyCode: String,
                   sessionKey: String,
                   sessionDuration: Option[FiniteDuration] = 60.minutes.some)

  val loginParams: Map[String, Login] = Company.map { domain =>
    Login(domain, domain + ".login")
  }.groupBy(_.companyCode).map { case (code, value) =>
    code -> value.head
  }

}
