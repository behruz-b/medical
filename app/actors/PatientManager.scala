package actors

import akka.actor.Actor
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import javax.inject.Inject
import play.api.{Configuration, Environment}
import protocols.AppProtocol._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

class PatientManager @Inject()(val configuration: Configuration,
                               val environment: Environment)
  extends Actor with LazyLogging {

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  private val DoobieModule = DoobieUtil.doobieModule(configuration)

  override def preStart(): Unit = {
    logger.debug(s"++++++++++")
  }

  override def receive: Receive = {
    case CreatePatients(patient) =>
      sender () ! createPatient(patient)
  }

  private def createPatient(patient: Patient): Patient = {
    logger.debug(s"creating patient: $patient")
    for {
      _ <- DoobieModule.repo.create(patient.firstName, patient.login).unsafeToFuture()
    } yield {
      logger.debug(s"patient: $patient")
    }
    patient
  }
}
