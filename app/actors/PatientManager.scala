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

  override def receive: Receive = {
    case CreatePatient(patient) =>
      createPatient(patient).pipeTo(sender())
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
}
