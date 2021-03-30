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

class StatsManagerStub @Inject()(val configuration: Configuration,
                                 val environment: Environment)
                                (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)

  override def receive: Receive = {
    case AddStatsAction(statsAction) =>
      addStatsAction(statsAction).pipeTo(sender())

    case GetStats =>
      getStats.pipeTo(sender())
  }

  private def addStatsAction(statsAction: StatsAction): Future[Either[String, String]] = {
    repo.addStatsAction(statsAction).unsafeToFuture().map { _ =>
      Right("Successfully added")
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while create patient.", error)
        Left("Error happened while creating patient")
    }
  }

  private def getStats: Future[List[StatsAction]] = {
    repo.getStats.unsafeToFuture()
  }
}
