package doobie.common

import cats.effect._
import doobie.config.DatabaseConfig
import doobie.repository.RepositoryInterpreter
import play.api.Configuration

class DoobieModule[F[_]: Effect : ContextShift](configuration: Configuration) {

  val transactor = DatabaseConfig.dbTransactor(configuration)

  val repo = new RepositoryInterpreter[F](transactor)

}
