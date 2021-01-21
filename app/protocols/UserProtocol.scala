package protocols

import play.api.libs.json._

import java.time.LocalDateTime

object UserProtocol {

  case class User(
    created_at: LocalDateTime,
    firstname: String,
    lastname: String,
    phone: String,
    role: String,
    company_code: String,
    login: String,
    password: String,
  ) {
    def id: Option[Int] = None
  }
  implicit val userFormat: OFormat[User] = Json.format[User]

  case class CheckUserByLogin(login: String, password: String)
  case class CreateUser(user: User)
}
