package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.PatientRepositoryAlgebra
import doobie.implicits._
import protocols.AppProtocol.Patient
import doobie.implicits.javasql._
import doobie.util.Read

import java.sql.Timestamp
import java.time.LocalDateTime

object MessageSQL extends CommonSQL  {

  implicit val han: LogHandler = LogHandler.jdkLogHandler
  implicit val patientRead: Read[Patient] =
    Read[(Timestamp, String, String, String, Option[String], String, String, String, String, Option[String])].map {
      case (created_at, firstname, lastname, phone, email, passport, customer_id, login, password, lab_image) =>
        Patient(created_at.toLocalDateTime, firstname, lastname, phone, email, passport, customer_id, login, password, lab_image)
    }

  private def javaLdTime2JavaSqlTimestamp(ldTime: LocalDateTime): Timestamp = {
    Timestamp.valueOf(ldTime)
  }

  def create(patient: Patient): doobie.ConnectionIO[Int] = {
    val values = fr"(${javaLdTime2JavaSqlTimestamp(patient.created_at)},${patient.firstname}, ${patient.lastname}, ${patient.phone}, ${patient.email}, ${patient.passport}, ${patient.customer_id}, ${patient.login}, ${patient.password})"

    sql"""insert into "Patients" (created_at, firstname, lastname, phone, email, passport, customer_id, login, password)
          values $values""".update.withUniqueGeneratedKeys[Int]("id")
  }

  def addAnalysisResult(customerId: String, analysisFileName: String): Update0 = {
    sql"""UPDATE "Patients" SET analysis_image_name=$analysisFileName
          WHERE customer_id=$customerId""".update
  }

  def getByCustomerId(customerId: String): Query0[Patient] = {
    val querySql = fr"""SELECT created_at,firstname,lastname,phone,email,passport,customer_id,login,password,analysis_image_name FROM "Patients" WHERE customer_id = $customerId"""
    querySql.query[Patient]
  }

  def getPatientByLogin(login: String): doobie.Query0[Patient] = {
    val querySql = fr"""select created_at,firstname,lastname,phone,email,passport,customer_id,login,password,analysis_image_name from "Patients" WHERE login = $login"""
      querySql.query[Patient]
  }

  def getPatients: ConnectionIO[List[Patient]] = {
    val querySql = fr"""SELECT created_at,firstname,lastname,phone,email,passport,customer_id,login,password,analysis_image_name FROM "Patients" ORDER BY created_at """
    querySql.query[Patient].to[List]
  }
}

class RepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](override val xa: Transactor[F])
  extends CommonRepositoryInterpreter[F](xa) with PatientRepositoryAlgebra[F] {

  override val commonSql: CommonSQL = MessageSQL

}

object RepositoryInterpreter  {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): RepositoryInterpreter[F] =
    new RepositoryInterpreter(xa)
}