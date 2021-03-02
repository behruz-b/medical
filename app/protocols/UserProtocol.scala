package protocols

import play.api.libs.json._

import java.time.LocalDateTime
import play.api.libs.functional.syntax.toFunctionalBuilderOps

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

  case class ChangePassword(login: String, newPass: String)

  implicit val doctorFormReads: Reads[ChangePassword] = (
      (__ \ "login").read[String] and
      (__ \ "newPass").read[String]
    ) (ChangePassword)

  case class CheckUserByLogin(login: String, password: String)
  case class CreateUser(user: User)
  case class CheckUserByLoginAndCreate(user: User)
  case class CheckSmsDeliveryStatusDoc(requestId: String)
  case class SendSmsToDoctor(customerId: String)
  case class Roles(id: Int, name: String, code: String)
  case object GetRoles
  implicit val rolesFormat: OFormat[Roles] = Json.format[Roles]

  val SmsTextDoc: (String, String, String) => String = (customerId: String, hostName: String, welcomeText: String) =>
    s"$welcomeText\\nSizning bemoringizning tibbiy xulosasi tayyor.\\nTibbiy xulosani" +
      s"quyidagi havola orqali olishingiz mumkin:\\nhttp://$hostName.uz/r/$customerId"

  def getSmsTextForUserCreation(role: String, login: String, password: String, welcomeText: String): String = {
    s"$welcomeText\\nSizga '${role.toUpperCase}' berildi:\\n" +
      s"Login: $login\\nParol: $password"
  }
}
