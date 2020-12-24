package actors

import akka.actor.Actor
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import protocols.AppProtocol._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class PatientManager @Inject()(implicit val ec: ExecutionContext) extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(30.seconds)

  override def receive: Receive = {
    case CreatePatients(patient) =>
      sender() ! createPatient(patient)
  }

  private def createPatient(patient: Patient): Patient = {
    logger.debug(s"patient: $patient")
    patient
  }
}
// TODO test pullRequest
