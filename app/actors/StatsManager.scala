package actors

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import protocols.PatientProtocol._
import util.StringUtil

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class StatsManager @Inject()(val configuration: Configuration,
                             val environment: Environment,
                             )
                            (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)
//  private val DoobieModule = DoobieUtil.doobieModule(configuration)


  override def receive: Receive = {
    ???
  }



}
