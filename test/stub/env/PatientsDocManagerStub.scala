package stub.env

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import play.api.{Configuration, Environment}
import protocols.PatientProtocol._
import stub.env.DoobieModuleStub.repo

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class PatientsDocManagerStub @Inject()(val configuration: Configuration,
                                       val environment: Environment)
                                      (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)

  override def receive: Receive = {
    case AddPatientsDoc(patientsDoc) =>
      addPatientsDoc(patientsDoc).pipeTo(sender())

    case GetPatientsDoc =>
      getPatientsDoc.pipeTo(sender())
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

  private def getPatientsDoc: Future[List[GetPatientsDocById]] = {
    repo.getPatientsDoc.unsafeToFuture()
  }
}
