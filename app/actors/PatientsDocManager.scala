package actors

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import javax.inject.Inject
import play.api.{Configuration, Environment}
import protocols.PatientProtocol._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class PatientsDocManager @Inject()(val configuration: Configuration,
                                   val environment: Environment)
                                  (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)
  private val DoobieModule = DoobieUtil.doobieModule(configuration)

  override def receive: Receive = {
    case AddPatientsDoc(patientsDoc) =>
      addPatientsDoc(patientsDoc).pipeTo(sender())

    case GetPatientsDoc =>
      getPatientsDoc.pipeTo(sender())
  }

  private def addPatientsDoc(patientsDoc: PatientsDoc): Future[Either[String, String]] = {
    DoobieModule.repo.addPatientsDoc(patientsDoc).unsafeToFuture().map { _ =>
      Right("Successfully added")
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while create patient.", error)
        Left("Error happened while creating patient")
    }
  }

  private def getPatientsDoc: Future[List[GetPatientsDocById]] = {
    DoobieModule.repo.getPatientsDoc.unsafeToFuture()
  }
}
