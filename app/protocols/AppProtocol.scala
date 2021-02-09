package protocols

import play.api.libs.mailer.{Email, SMTPConfiguration}

object AppProtocol {
  case class SendMail(email: Email, smtpConfig: SMTPConfiguration, recipients: Vector[String])
  case class SendSingleMail(email: Email, smtpConfig: SMTPConfiguration)
}
