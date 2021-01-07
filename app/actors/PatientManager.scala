package actors

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import play.api.{Configuration, Environment}
import protocols.AppProtocol._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

class PatientManager @Inject()(val configuration: Configuration,
                               val environment: Environment)
                              (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

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
      getPatientByCustomerId(customerId)

    case GetPatientByLogin(login, password) =>
      getPatientByLogin(login, password).pipeTo(sender())

    case GetPatients =>
      getPatients.pipeTo(sender())
  }

  private def createPatient(patient: Patient): Future[Either[String, String]] = {
    (for {
      result <- DoobieModule.repo.create(patient).unsafeToFuture()
    } yield {
      logger.debug(s"result: $result")
      Right("Successfully added")
    }).recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while creating patient")
    }
  }

  private def getPatientByCustomerId(customerId: String): Future[Either[String, Option[Patient]]] = {
    (for {
      patient <- DoobieModule.repo.getByCustomerId(customerId).compile.last.unsafeToFuture()
    } yield {
      logger.debug(s"result: $patient")
      Right(patient)
    }).recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while requesting patient")
    }
  }

  private def getPatientByLogin(login: String, password: String): Future[Either[String, String]] = {
    (for {
      result <- DoobieModule.repo.getPatientByLogin(login).compile.last.unsafeToFuture()
    } yield {
      logger.debug(s"result: ${result.map(_.password == password)}, $result")
      Right("Successfully")
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
      logger.debug(s"result: $result")
      if (result == 1) {
        Right("Successfully")
      } else {
        Left("Error while adding Analysis File to DB")
      }
    }).recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while adding Analysis File to DB")
    }
  }
  private def getPatients: Future[List[Patient]] = {
    for {
      patients <- DoobieModule.repo.getPatients.unsafeToFuture()
    } yield {
      logger.debug(s"result: $patients")
      patients
    }
  }
}
