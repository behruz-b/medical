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
  case class CheckUserByLoginAndCreate(user: User)
  case class CheckSmsDeliveryStatusDoc(requestId: String)
  case class SendSmsToDoctor(customerId: String)
  case class Roles(id: Int, name: String, code: String)
  case object GetRoles
  implicit val rolesFormat: OFormat[Roles] = Json.format[Roles]

  val SmsTextDoc: String => String = (customerId: String) =>
    s"'Elegant Farm' Diagnostika Markazi Sizning bemoringizning tibbiy xulosasi tayyor.\\n Tibbiy xulosani" +
      s"quyidagi havola orqali olishingiz mumkin:\\n http://elegant-farm.uz/r/$customerId"
}
