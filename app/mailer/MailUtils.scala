package mailer

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import play.api.Configuration
import play.api.libs.mailer.{Email, SMTPConfiguration, SMTPMailer}

import scala.concurrent.{ExecutionContext, Future}


object MailUtils extends LazyLogging {

  def smtpConfig(configuration: Configuration, path: String): SMTPConfiguration = {
    SMTPConfiguration(configuration.get[Configuration](path).underlying)
  }

  def smtpConfig(config: Config, path: String): SMTPConfiguration = {
    SMTPConfiguration(config.getConfig(path))
  }

  def sendSingleMailSync(data: Email, smtpConfig: SMTPConfiguration): Unit = {
    val startedAt = System.currentTimeMillis()

    try {
      val mailer = new SMTPMailer(smtpConfig)

      logger.debug(s"Starting sending single email: from [${data.from}] subject [${data.subject}] to ${data.to.size} recipients")
      mailer.send(data)
      logger.debug(s"Finished sending single email: from [${data.from}] subject [${data.subject}]")
    } catch {
      case error: Exception =>
        logger.error(s"Error while sending single email: from [${data.from}] subject [${data.subject}] recipient [${data.to}]", error)
    }

    logger.info(f"Email sending finished in ${(System.currentTimeMillis - startedAt) / 1000.0}%.2f seconds")
  }

  def sendMailSync(data: Email, smtpConfig: SMTPConfiguration, recipients: Seq[String]): Unit = {
    val startedAt = System.currentTimeMillis()

    recipients.zipWithIndex.foreach { case (recipient, index) =>
      try {
        val mailer = new SMTPMailer(smtpConfig)

        logger.debug(s"Starting sending email ${index + 1}/${recipients.size}: from [${data.from}] subject [${data.subject}]")
        mailer.send(data.copy(to = Seq(recipient)))
        logger.debug(s"Finished sending email ${index + 1}/${recipients.size}: from [${data.from}] subject [${data.subject}]")
      } catch {
        case error: Exception =>
          logger.error(s"Error while sending email: from [${data.from}] subject [${data.subject}] recipients [$recipients], failed recipient [$recipient]", error)
      }
    }

    logger.info(f"Email sending to ${recipients.size} recipients finished in ${(System.currentTimeMillis - startedAt) / 1000.0}%.2f seconds")
  }

  def sendMailAsync(data: Email, smtpConfig: SMTPConfiguration, recipients: Seq[String])
                   (implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      sendMailSync(data, smtpConfig, recipients)
    }
  }

  def sendSingleMailAsync(data: Email, smtpConfig: SMTPConfiguration)
                         (implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      sendSingleMailSync(data, smtpConfig)
    }
  }

}
