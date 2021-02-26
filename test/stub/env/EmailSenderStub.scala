package stub.env

import akka.actor.Actor
import akka.pattern.pipe
import com.typesafe.scalalogging.LazyLogging
import DoobieModuleStub.repo
import protocols.PatientProtocol._
import protocols.SecurityUtils.md5

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailSenderStub extends Actor with LazyLogging {

  override def receive: Receive = {
    case CreatePatient(patient) =>
      createPatient(patient).pipeTo(sender())
  }

  private def createPatient(patient: Patient): Future[Either[String, String]] = {
    repo.create(patient.copy(password = md5(patient.password))).unsafeToFuture().map { _ =>
      Right("Successfully added")
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while create patient.", error)
        Left("Bemorni ro'yhatga olishda xatolik yuz berdi. Iltimos qayta harakat qilib ko'ring!")
    }
  }

}
