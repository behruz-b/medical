package actors

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import cats.implicits.catsSyntaxOptionId
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import play.api.http.Status.OK
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import protocols.PatientProtocol._
import protocols.SecurityUtils.md5
import util.StringUtil

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class PatientManager @Inject()(val configuration: Configuration,
                               val environment: Environment,
                               ws: WSClient)
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

  // For testing purpose test DB
  //  override def preStart: Unit = {
  //    self ! AddAnalysisResult("U-668", "Sample Image Name of Analysis")
  //    self ! CheckSmsDeliveryStatus("430349076")
  //  }

  override def receive: Receive = {
    case CreatePatient(patient) =>
      createPatient(patient).pipeTo(sender())

    case AddAnalysisResult(customerId, analysisFileName) =>
      addAnalysisResult(customerId, analysisFileName).pipeTo(sender())

    case AddSmsLinkClick(customerId, smsLinkClick) =>
      addSmsLinkClick(customerId, smsLinkClick).pipeTo(sender())

    case GetPatientByCustomerId(customerId) =>
      getPatientByCustomerId(customerId).pipeTo(sender())

    case GetPatientByLogin(login, password) =>
      getPatientByLogin(login, password).pipeTo(sender())

    case GetPatientsForm(analyseType) =>
      getPatients(analyseType).pipeTo(sender())

    case SendSmsToCustomer(customerId) =>
      sendSMS(customerId).pipeTo(sender())

    case CheckCustomerId(customerId) =>
      checkCustomerId(customerId).pipeTo(sender())

    case SendIdToPatientViaSms(customerId) =>
      sendIdToPatientViaSms(customerId).pipeTo(sender())

    case CheckSmsDeliveryStatus(requestId, customerId) =>
      checkSmsDeliveryStatus(requestId, customerId).pipeTo(sender())

  }

  private def createPatient(patient: Patient): Future[Either[String, String]] = {
    (for {
      id <- DoobieModule.repo.addPatientsDoc(PatientsDoc(patient.docFullName.getOrElse(""), patient.docPhone.getOrElse(""))).unsafeToFuture()
      _ <- DoobieModule.repo.create(patient.copy(password = md5(patient.password), docId = id.some)).unsafeToFuture()
    } yield {
      Right("Successfully added")
    }).recover {
      case error: Throwable =>
        logger.error("Error occurred while create patient.", error)
        Left("Bemorni ro'yhatga olishda xatolik yuz berdi. Iltimos qayta harakat qilib ko'ring!")
    }
  }

  private def getPatientByCustomerId(customerId: String): Future[Either[String, Patient]] = {
    DoobieModule.repo.getByCustomerId(customerId).compile.last.unsafeToFuture().map { patient =>
      if (patient.isDefined) {
        Right(patient.get)
      } else {
        Left("Error happened while requesting patient")
      }
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while get patient by customer id", error)
        Left("Error happened while requesting patient")
    }
  }

  private def checkCustomerId(customerId: String): Future[Either[String, Patient]] = {
    DoobieModule.repo.getByCustomerId(customerId).compile.last.unsafeToFuture().map { patient =>
      if (patient.isDefined) {
        Right(patient.get)
      } else {
        Left("Error happened while requesting patient")
      }
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while get patient by customer id", error)
        Left("Error happened while requesting patient")
    }
  }

  private def getPatientByLogin(login: String, password: String): Future[Either[String, String]] = {
    DoobieModule.repo.getPatientByLogin(login).compile.last.unsafeToFuture().map { result =>
      if (result.exists(_.password == md5(password))) {
        Right("Successfully")
      } else {
        Left("Incorrect login or password")
      }
    }.recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while requesting Login or Password")
    }
  }

  private def addAnalysisResult(customerId: String, analysisFileName: String): Future[Either[String, String]] = {
    DoobieModule.repo.addAnalysisResult(customerId, analysisFileName).unsafeToFuture().map { result =>
      if (result == 1) {
        Right("Successfully")
      } else {
        Left("Error while adding Analysis File to DB")
      }
    }.recover {
      case e: Throwable =>
        logger.error("Error occurred while add analysis result.", e)
        Left("Error happened while adding Analysis File to DB")
    }
  }

  private def addSmsLinkClick(customerId: String, smsLinkClick: String): Future[Either[String, String]] = {
    DoobieModule.repo.addSmsLinkClick(customerId, smsLinkClick).unsafeToFuture().map { result =>
      if (result == 1) {
        Right("Successfully")
      } else {
        Left("Error while adding Analysis File to DB")
      }
    }.recover {
      case e: Throwable =>
        logger.error("Error occurred while add analysis result.", e)
        Left("Error happened while adding Analysis File to DB")
    }
  }

  private def getPatients(analyseType: Option[String]): Future[Either[String, List[Patient]]] = {
    DoobieModule.repo.getPatients(analyseType).unsafeToFuture().map { patient =>
      Right(patient)
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while get patient by customer id", error)
        Left("Error happened while requesting patient")
    }
  }

  private def sendSMS(customerId: String): Future[Either[String, String]] = {
    getPatientByCustomerId(customerId).flatMap {
      case Right(p) =>
        actualSendingSMS(p.phone, SmsText(customerId, HostName), customerId)
      case Left(e) =>
        logger.error(s"Error happened", e)
        Future.successful(Left("Error occurred while sending SMS to Customer"))
    }
  }

  private def sendIdToPatientViaSms(customerId: String): Future[Either[String, String]] = {
    getPatientByCustomerId(customerId).flatMap {
      case Right(p) =>
        actualSendingSMS(p.phone, SmsTextForPatientId(customerId), customerId)
      case Left(e) =>
        logger.error(s"Error happened", e)
        Future.successful(Left("Error occurred while sending SMS to Customer"))
    }
  }

  private def actualSendingSMS(phone: String, smsText: String, customerId: String): Future[Either[String, String]] = {
    logger.debug(s"SMS API: ${StringUtil.maskMiddlePart(SmsApi, 10)}, SMS Login: ${StringUtil.maskMiddlePart(SmsLogin, 1, 1)}, SMS Password: ${StringUtil.maskMiddlePart(SmsPassword)}")
    val data = s"""login=$SmsLogin&password=$SmsPassword&data=[{"phone":"$phone","text":"$smsText"}]"""
    val result = ws.url(SmsApi)
      .withRequestTimeout(15.seconds)
      .withHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(data)
    result.map { r =>
      val body = r.json(0)
      logger.debug(s"SMS API Result: $body")
      r.status match {
        case OK =>
          val id = (body \ "request_id").asOpt[Int]
          if (id.isDefined) {
            logger.debug(s"RequestId: $id")
            context.system.scheduler.scheduleOnce(5.seconds, self, CheckSmsDeliveryStatus(id.get.toString, customerId))
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
        Left("Error while sending SMS")
    }
  }

  private def checkSmsDeliveryStatus(requestId: String, customerId: String) = {
    logger.debug(s"Checking SMS Delivery status...")
    val data = s"""login=$SmsLogin&password=$SmsPassword&data=[{"request_id":"$requestId"}]"""
    val result = ws.url(apiStatus)
      .withRequestTimeout(15.seconds)
      .withHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(data)
    result.map { deliveryStatus =>
      val body = deliveryStatus.json
      logger.debug(s"SMS deliveryStatus: $body")
      deliveryStatus.status match {
        case OK =>
          val deliveryNotification = ((body \ "messages") (0) \ "status").as[String]
          DoobieModule.repo.addDeliveryStatus(customerId, deliveryNotification).unsafeToFuture()
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
