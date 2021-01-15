package actors

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import play.api.{Configuration, Environment}
import protocols.PatientProtocol._
import javax.inject.Inject

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class StatsManager @Inject()(val configuration: Configuration,
                             val environment: Environment,
                             )
                            (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)
  private val DoobieModule = DoobieUtil.doobieModule(configuration)

  override def receive: Receive = {
    case AddStatsAction(statsAction) =>
      addStatsAction(statsAction).pipeTo(sender())
}

  private def addStatsAction(statsAction: StatsAction): Future[Either[String, String]] = {
    (for {
      _ <- DoobieModule.repo.addStatsAction(statsAction).unsafeToFuture()
    } yield {
      Right("Successfully added")
    }).recover {
      case error: Throwable =>
        logger.error("Error occurred while create patient.", error)
        Left("Error happened while creating patient")
    }
  }
}
