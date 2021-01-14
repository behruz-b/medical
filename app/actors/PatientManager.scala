package actors

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import protocols.AppProtocol._

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class PatientManager @Inject()(val configuration: Configuration,
                               val environment: Environment,
                               ws: WSClient)
                              (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  private val SmsProviderConfig = configuration.get[Configuration]("sms-provider")
  private val Url: String = SmsProviderConfig.get[String]("url")
  private val Login: String = SmsProviderConfig.get[String]("login")
  private val Password: String = SmsProviderConfig.get[String]("password")
  implicit val defaultTimeout: Timeout = Timeout(60.seconds)
  private val DoobieModule = DoobieUtil.doobieModule(configuration)

  // For testing purpose test DB
  //  override def preStart: Unit = {
  //    self ! AddAnalysisResult("U-668", "Sample Image Name of Analysis")
  //  }

  override def receive: Receive = {
    case CreatePatient(patient) =>
      createPatient(patient).pipeTo(sender())

    case AddAnalysisResult(customerId, analysisFileName) =>
      addAnalysisResult(customerId, analysisFileName).pipeTo(sender())

    case GetPatientByCustomerId(customerId) =>
      getPatientByCustomerId(customerId).pipeTo(sender())

    case GetPatientByLogin(login, password) =>
      getPatientByLogin(login, password).pipeTo(sender())

    case GetPatients =>
      getPatients.pipeTo(sender())
  }

  private def createPatient(patient: Patient): Future[Either[String, String]] = {
    (for {
      _ <- DoobieModule.repo.create(patient).unsafeToFuture()
    } yield {
      Right("Successfully added")
    }).recover {
      case error: Throwable =>
        logger.error("Error occurred while create patient.", error)
        Left("Error happened while creating patient")
    }
  }

  private def getPatientByCustomerId(customerId: String): Future[Either[String, Patient]] = {
    (for {
      patient <- DoobieModule.repo.getByCustomerId(customerId).compile.last.unsafeToFuture()
    } yield {
      Right(patient.get)
    }).recover {
      case error: Throwable =>
        logger.error("Error occurred while get patient by customer id.", error)
        Left("Error happened while requesting patient")
    }
  }

  private def getPatientByLogin(login: String, password: String): Future[Either[String, String]] = {
    (for {
      result <- DoobieModule.repo.getPatientByLogin(login).compile.last.unsafeToFuture()
    } yield {
      logger.debug(s"result: ${result.exists(_.password == password)}, $result")
      if (result.exists(_.password == password)) {
        Right("Successfully")
      } else {
        Left("Incorrect login or password")
      }
    }).recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while requesting Login or Password")
    }
  }

  private def addAnalysisResult(customerId: String, analysisFileName: String): Future[Either[String, String]] = {
    (for {
      result <- DoobieModule.repo.addAnalysisResult(customerId, analysisFileName).unsafeToFuture()
    } yield {
      if (result == 1) {
        Right("Successfully")
      } else {
        Left("Error while adding Analysis File to DB")
      }
    }).recover {
      case e: Throwable =>
        logger.error("Error occurred while add analysis result.", e)
        Left("Error happened while adding Analysis File to DB")
    }
  }

  private def getPatients: Future[List[Patient]] = {
    for {
      patients <- DoobieModule.repo.getPatients.unsafeToFuture()
    } yield {
      patients
    }
  }

  /**
   * @param number is the phone number to which the sms should be sent
   * @param text   is the text of the message to be sent sms
   * @return Sms status via
   * {{{case class SmsStatus}}}
   */

  private def sendSMS(number: String, text: String): Future[String] = {
    val data = Json.obj("phone" -> number, "text" -> text)
    for {
      result <- ws.url(Url)
        .addHttpHeaders("login" -> Login)
        .addHttpHeaders("login" -> Password)
        .post(data)
      status <- ws.url(Url)
        .addHttpHeaders("login" -> Login)
        .addHttpHeaders("login" -> Password)
        .post(result.body)
    } yield status.body
  }
}
