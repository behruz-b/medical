package actors

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import play.api.Configuration

import javax.inject.Inject
import protocols.AppProtocol._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

class PatientManager @Inject()(implicit val ec: ExecutionContext, val configuration: Configuration) extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)
  private val DoobieModule = DoobieUtil.doobieModule(configuration)

  override def receive: Receive = {
    case CreatePatients(patient) =>
      sender () ! createPatient(patient)
  }

  private def createPatient(patient: Patient): Patient = {
    for {
      _ <- DoobieModule.repo.create(patient.firstName, patient.login).unsafeToFuture()
    } yield {
      logger.debug(s"patient: $patient")
    }
    patient
  }
}
