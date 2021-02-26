package stub.env

import cats.effect.{Blocker, ContextShift, IO}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import doobie.Transactor
import doobie.implicits._
import doobie.repository.RepositoryInterpreter
import doobie.util.fragment.Fragment

import scala.concurrent.ExecutionContext
import scala.io.Source

object DoobieModuleStub {
  private var postgres: EmbeddedPostgres = _
  private var transactor: Transactor[IO] = _

  def getScriptsInFile: Array[String] = {
    val source = Source.fromResource("db.sql")
    try {
      source.mkString.split(";")
    } catch {
      case err: java.io.IOException =>
        println(s"error: $err")
        Array.empty[String]
    } finally {
      source.close
    }
  }

  implicit private val ioContextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  val inserts_sql: Array[String] = getScriptsInFile

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
