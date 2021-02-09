package actors

import akka.actor.Actor
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.mailer.{Email, SMTPConfiguration, SMTPMailer}
import play.api.{Configuration, Environment}
import protocols.AppProtocol.{SendMail, SendSingleMail}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

class EmailSender @Inject()(val configuration: Configuration,
                            val environment: Environment)
                           (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)

  override def receive: Receive = {
    case SendMail(email, smtpConfig, recipients) =>
      sendMail(email, smtpConfig, recipients)

    case SendSingleMail(email, smtpConfig) =>
      sendSingleMail(email, smtpConfig)
  }

  private def sendMail(email: Email, smtpConfig: SMTPConfiguration, recipients: Vector[String]): Int = {
    recipients.foldLeft(0) { case (time, recipient) =>
      val sendEmail = SendSingleMail(email.copy(to = Seq(recipient)), smtpConfig)
      val next = time + 3
      context.system.scheduler.scheduleOnce(next.seconds, self, sendEmail)
      next
    }
  }

  private def sendSingleMail(email: Email, smtpConfig: SMTPConfiguration): Unit = {
    Try {
      val mailer = new SMTPMailer(smtpConfig)

      logger.debug(s"Starting sending single email: from [${email.from}] subject [${email.subject}] to ${email.to.size} recipients")
      mailer.send(email)
    } match {
      case Failure(error) =>
        logger.error(s"Error while sending single email: from [${email.from}] subject [${email.subject}] recipient [${email.to}]", error)
      case Success(_) =>
        logger.debug(s"Finished sending single email: from [${email.from}] subject [${email.subject}]")
    }
  }
}