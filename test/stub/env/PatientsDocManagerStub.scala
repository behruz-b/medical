package stub.env

import akka.actor.Actor
import akka.pattern.pipe
import com.typesafe.scalalogging.LazyLogging
import protocols.PatientProtocol._
import stub.env.DoobieModuleStub.repo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PatientsDocManagerStub extends Actor with LazyLogging {

  override def receive: Receive = {
    case AddPatientsDoc(patientsDoc) =>
      addPatientsDoc(patientsDoc).pipeTo(sender())
  }

  private def addPatientsDoc(patientsDoc: PatientsDoc): Future[Either[String, String]] = {
    repo.addPatientsDoc(patientsDoc).unsafeToFuture().map { _ =>
      Right("Successfully added")
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while create patient.", error)
        Left("Error happened while creating patient")
    }
  }

}
