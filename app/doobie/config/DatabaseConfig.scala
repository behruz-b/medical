package doobie.config

import cats.effect.{Async, ContextShift}
import com.typesafe.config.Config
import doobie.util.transactor.Transactor
import play.api.{ConfigLoader, Configuration}

case class DatabaseConfig(url: String, driver: String, user: String, password: String)

object DatabaseConfig {

  implicit val dbConfigLoader = new ConfigLoader[DatabaseConfig] {
    def load(config: Config, path: String): DatabaseConfig = {
      def getString(key: String) = config.getString(s"$path.$key")

      DatabaseConfig(
        driver = getString("driver"),
        url = getString("url"),
        user = getString("username"),
        password = getString("password")
      )
    }
  }

  def dbTransactor[F[_]: Async : ContextShift](configuration: Configuration): Transactor[F] = {
    val dbc = configuration
      .get[Configuration]("db")
      .get[DatabaseConfig]("default")

    Transactor.fromDriverManager[F](dbc.driver, dbc.url, dbc.user, dbc.password)
  }

}
