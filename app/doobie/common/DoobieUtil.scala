package doobie.common

import cats.effect.{ContextShift, IO}
import play.api.Configuration

import scala.concurrent.ExecutionContext

object DoobieUtil {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def doobieModule(configuration: Configuration) = new DoobieModule[IO](configuration)

}
