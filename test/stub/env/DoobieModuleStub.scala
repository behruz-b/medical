package stub.env

import cats.effect.{Blocker, ContextShift, IO}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import doobie.Transactor
import doobie.implicits._
import doobie.repository.RepositoryInterpreter
import doobie.util.fragment.Fragment

import scala.concurrent.ExecutionContext
import scala.io.{BufferedSource, Source}

object DoobieModuleStub {
  private var postgres: EmbeddedPostgres = _
  private var transactor: Transactor[IO] = _

  implicit private val ioContextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  val source: BufferedSource = Source.fromResource("db.sql")
  val inserts_sql = source.mkString.split(";")
  source.close()

  def dbTransactor: Transactor[IO] = {
    postgres = EmbeddedPostgres.builder().start()
    transactor = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      postgres.getJdbcUrl("postgres", "postgres"),
      "postgres",
      "postgres",
      Blocker.liftExecutionContext(ExecutionContext.global)
    )
    inserts_sql.foreach { sql =>
      Fragment.const(sql).update.run
        .transact(transactor)
        .unsafeRunSync()
    }

    transactor
  }

  val repo = new RepositoryInterpreter(dbTransactor)

}
