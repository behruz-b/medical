package stub.env

import actors.MonitoringNotifier.MonitoringMailer
import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import cats.implicits.catsSyntaxOptionId
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.mailer.{Email, SMTPConfiguration}
import play.api.{Configuration, Environment}
import protocols.AppProtocol
import protocols.AppProtocol.{NotifyMessage, SendMail}

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object MonitoringNotifier {

  case class MonitoringMailer(
                               toEmailAddresses: Seq[String],
                               fromEmailAddress: String,
                               smtpConfig: SMTPConfiguration
                             )

}

class MonitoringNotifierStub @Inject()(val configuration: Configuration,
                                       val environment: Environment,
                                       @Named("email-sender") val emailSender: ActorRef)
                                      (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)

  override def receive: Receive = {
    case NotifyMessage(message) =>
      sendEmail(message)
  }

  private def monitoringMailer: MonitoringMailer = {
    val mailerConf = configuration.get[Configuration]("monitoring")
    val toEmailAddresses = mailerConf.get[Seq[String]]("recipients")
    val fromEmailAddress = mailerConf.get[String]("sender.from-address")
    MonitoringMailer(
      toEmailAddresses = toEmailAddresses,
      fromEmailAddress = fromEmailAddress,
      smtpConfig = AppProtocol.smtpConfig(mailerConf, path = "sender.play.mailer")
    )
  }

  private def sendEmail(message: String): Unit = {
    val subject = "Medical Alert"
    val email = Email(
      subject = subject,
      from = monitoringMailer.fromEmailAddress,
      to = monitoringMailer.toEmailAddresses,
      bodyText = message.some,
    )
    emailSender ! SendMail(email, monitoringMailer.smtpConfig, monitoringMailer.toEmailAddresses)
  }
}