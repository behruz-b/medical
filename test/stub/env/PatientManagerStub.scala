package stub.env

import akka.actor
import akka.actor.{Actor, Props}
import akka.pattern.pipe
import com.typesafe.scalalogging.LazyLogging
import protocols.AppProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PatientManagerStub {
  def props: Props = actor.Props(new PatientManagerStub)
}

class PatientManagerStub extends Actor with LazyLogging {

  override def receive: Receive = {
    case CreatePatient(patient) =>
      createPatient(patient).pipeTo(sender())
  }

  def createPatient(patient: Patient): Future[Either[String, String]] = {
    println(s"FAKE_PATIENT:$patient")
    Future.successful(Right("SUCCESS"))
  }

}
