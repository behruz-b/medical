package actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import protocols.PatientProtocol.{AddStatsAction, Patient, StatsAction}
import protocols.SecurityUtils.md5
import protocols.UserProtocol.{CheckSmsDeliveryStatusDoc, CheckUserByLogin, CheckUserByLoginAndCreate, GetRoles, Roles,
  SendSmsToDoctor, SmsTextDoc, User, getSmsTextForUserCreation, ChangePassword}
import util.StringUtil

import java.time.LocalDateTime
import javax.inject.{Inject, Named}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class UserManager @Inject()(val configuration: Configuration,
                            val environment: Environment,
                            ws: WSClient,
                            @Named("stats-manager") val statsManager: ActorRef)
                           (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)
  private val DoobieModule = DoobieUtil.doobieModule(configuration)
  private val smsConfig = configuration.get[Configuration]("sms_config")
  private val SmsApi = smsConfig.get[String]("api")
  private val apiStatus = smsConfig.get[String]("api_status")
  private val SmsLogin = smsConfig.get[String]("login")
  private val SmsPassword = smsConfig.get[String]("password")
  private val HostName = configuration.get[String]("HostName")
  private val WelcomeText = configuration.get[String]("WelcomeText")

  override def receive: Receive = {
    case CheckUserByLogin(login, password) =>
      checkUserByLoginAndPassword(login, password).pipeTo(sender())

    case CheckUserByLoginAndCreate(user) =>
      checkUserByLoginAndCreate(user).pipeTo(sender())

    case GetRoles =>
      getRoles.pipeTo(sender())

    case CheckSmsDeliveryStatusDoc(requestId) =>
      checkSmsDeliveryStatus(requestId).pipeTo(sender())

    case SendSmsToDoctor(customerId) =>
      sendSMSToDoctor(customerId).pipeTo(sender())

    case ChangePassword(login,newPass) =>
      changePassword(login,newPass).pipeTo(sender())
  }

  private def changePassword(login: String, newPass: String): Future[Either[String, String]] = {
    DoobieModule.repo.changePassword(login,newPass).unsafeToFuture().map { result =>
      if (result == 1) {
        Right("Successfully updated")
      } else {
        Left("Error while adding user password to DB")
      }
    }.recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while requesting Login or Password")
    }
  }

  private def checkUserByLoginAndPassword(login: String, password: String): Future[Either[String, String]] = {
    DoobieModule.repo.getUserByLogin(login).compile.last.unsafeToFuture().map { result =>
      if (result.exists(_.password == md5(password))) {
        Right(result.get.role)
      } else {
        Left("Incorrect login or password")
      }
    }.recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while requesting Login or Password")
    }
  }

  private def checkUserByLoginAndCreate(user: User): Future[Either[String, String]] = {
    DoobieModule.repo.createUser(user.copy(password = md5(user.password))).unsafeToFuture().map { _ =>
      actualSendingSMS(user.phone, getSmsTextForUserCreation(user.role, user.login, user.password, WelcomeText))
      Right("Successfully created!")
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while create user. Error: ", error)
        if (error.getMessage.contains("duplicate")) {
          Left("Kiritilgan login avvaldan mavjud!")
        } else {
          Left("Foydalanuvchi yaratishda hatolik yuz berdi. Iltimos qaytadan urinib ko'ring!")
        }
    }
  }

  private def getRoles: Future[List[Roles]] = {
    DoobieModule.repo.getRoles.unsafeToFuture()
  }

  private def getPatientByCustomerId(customerId: String): Future[Either[String, Patient]] = {
    DoobieModule.repo.getByCustomerId(customerId).compile.last.unsafeToFuture().map { patient =>
      Right(patient.get)
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while get patient by customer id", error)
        Left("Error happened while requesting user")
    }
  }

  private def sendSMSToDoctor(customerId: String): Future[Either[String, String]] = {
    getPatientByCustomerId(customerId).flatMap {
      case Right(p) =>
        if (p.docPhone.isDefined) {
          val statsAction = StatsAction(LocalDateTime.now, "-", "doc_send_sms", "-", "-", "-")
          statsManager ! AddStatsAction(statsAction)
          actualSendingSMS(p.docPhone.get, SmsTextDoc(customerId, HostName, WelcomeText))
        } else {
          Future.successful(Right("Message not sent to doctor"))
        }
      case Left(e) =>
        logger.error(s"Error happened", e)
        Future.successful(Left("Error occurred while sending SMS to Doc"))
    }
  }

  private def actualSendingSMS(phone: String, smsText: String): Future[Either[String, String]] = {
    logger.debug(s"SMS API in UserManagement: ${StringUtil.maskMiddlePart(SmsApi, 10)}, SMS Login: ${StringUtil.maskMiddlePart(SmsLogin, 1, 1)}, SMS Password: ${StringUtil.maskMiddlePart(SmsPassword)}")
    val data = s"""login=$SmsLogin&password=$SmsPassword&data=[{"phone":"$phone","text":"$smsText"}]"""
    val result = ws.url(SmsApi)
      .withRequestTimeout(15.seconds)
      .withHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(data)
    result.map { r =>
      val body = r.json(0)
      logger.debug(s"SMS API Result doc: $body")
      r.status match {
        case 200 =>
          val id = (body \ "request_id").asOpt[Int]
          if (id.isDefined) {
            logger.debug(s"RequestId: $id")
            context.system.scheduler.scheduleOnce(5.seconds, self, CheckSmsDeliveryStatusDoc(id.get.toString))
            Right("Successfully sent")
          } else {
            val errorText = (body \ "text").asOpt[String]
            logger.error(s"Error occurred while sending SMS, error: ${errorText.getOrElse("Error Text undefined")}")
            Left("Error occurred while sending SMS")
          }
        case _ =>
          val errorText = (body \ "text").head.asOpt[String]
          logger.error(s"Error Text: $errorText")
          Left("Error happened")
      }
    }.recover {
      case e =>
        logger.error("Error occurred while sending SMS to sms provider", e)
        Left("Error while sending SMS to doc")
    }
  }

  private def checkSmsDeliveryStatus(requestId: String): Future[Unit] = {
    logger.debug(s"Checking SMS Delivery status doc...")
    val data = s"""login=$SmsLogin&password=$SmsPassword&data=[{"request_id":"$requestId"}]"""
    val result = ws.url(apiStatus)
      .withRequestTimeout(15.seconds)
      .withHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(data)
    result.map { deliveryStatus =>
      val body = deliveryStatus.json
      logger.debug(s"SMS deliveryStatus doc: $body")
      deliveryStatus.status match {
        case 200 =>
          val deliveryNotification = ((body \ "messages") (0) \ "status").as[String]
          logger.debug(s"Delivery Notification: $deliveryNotification")
        case _ =>
          val errorText = (body \ "text").head.asOpt[String]
          logger.debug(s"Error Text: $errorText")
      }
    }.recover {
      case e =>
        logger.error("Error occurred while sending SMS to sms provider", e)
    }
  }

}
