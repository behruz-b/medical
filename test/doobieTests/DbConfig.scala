package doobieTests

import cats.effect.{ContextShift, IO}
import doobie.util.transactor.Transactor
import pureconfig._
import pureconfig.generic.auto._

import java.nio.file.{Path, Paths}
import scala.concurrent.ExecutionContext

object DbConfig {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  case class Config(db: Default)
  case class Default(default: DataBaseParameters)
  case class DataBaseParameters(driver: String, url: String, username: String, password: String)
  val path: Path = Paths.get("medical/conf/application.test.conf")

  val config = ConfigSource.default(ConfigSource.file(path)).loadOrThrow[Config]

  val DbTransactor = Transactor.fromDriverManager[IO](
    config.db.default.driver,
    config.db.default.url,
    config.db.default.username,
    config.db.default.password
  )
}
