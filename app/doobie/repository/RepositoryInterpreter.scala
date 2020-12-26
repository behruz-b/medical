package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.CreatorRepositoryAlgebra
import doobie.implicits._
import protocols.AppProtocol.Patient
import doobie.implicits.javasql._

import java.sql.Timestamp
import java.time.LocalDateTime

object MessageSQL extends CommonSQL  {

  implicit val han: LogHandler = LogHandler.jdkLogHandler

  private def javaLdTime2JavaSqlTimestamp(ldTime: LocalDateTime): Timestamp = {
    Timestamp.valueOf(ldTime)
  }

  def create(patient: Patient): doobie.ConnectionIO[Int] = {
    val values = fr"(${javaLdTime2JavaSqlTimestamp(patient.create_at)},${patient.firstname}, ${patient.lastname}, ${patient.phone}, ${patient.email}, ${patient.passport}, ${patient.customer_id}, ${patient.login}, ${patient.password})"

    sql"""insert into "Patients" (created_at, firstname, lastname, phone, email, passport, customer_id, login, password)
          values $values""".update.withUniqueGeneratedKeys[Int]("id")
  }

}

class RepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](override val xa: Transactor[F])
  extends CommonRepositoryInterpreter[F](xa) with CreatorRepositoryAlgebra[F] {

  override val commonSql: CommonSQL = MessageSQL

}

object RepositoryInterpreter  {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): RepositoryInterpreter[F] =
    new RepositoryInterpreter(xa)
}