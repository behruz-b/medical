package protocols

import com.typesafe.config.Config
import play.api.Configuration
import play.api.libs.mailer.{Email, SMTPConfiguration}

object AppProtocol {
  def smtpConfig(configuration: Configuration, path: String): SMTPConfiguration = {
    SMTPConfiguration(configuration.get[Configuration](path).underlying)
  }

  def smtpConfig(config: Config, path: String): SMTPConfiguration = {
    SMTPConfiguration(config.getConfig(path))
  }

  case class SendMail(email: Email, smtpConfig: SMTPConfiguration, recipients: Seq[String])
  case class SendSingleMail(email: Email, smtpConfig: SMTPConfiguration)

  case class NotifyMessage(message: String)
}
