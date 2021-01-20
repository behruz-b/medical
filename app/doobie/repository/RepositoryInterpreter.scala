package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.PatientRepositoryAlgebra
import doobie.implicits._
import protocols.PatientProtocol._
import doobie.implicits.javasql._
import doobie.util.Read
import protocols.UserProtocol.User
import java.sql.Timestamp
import java.time.LocalDateTime

object MessageSQL extends CommonSQL  {

  implicit val han: LogHandler = LogHandler.jdkLogHandler
  implicit val patientRead: Read[Patient] =
    Read[(Timestamp, String, String, String, String, String, String, String, String, Timestamp, String, Option[String], Option[String], Option[String])].map {
      case (created_at, firstname, lastname, phone, customer_id, company_code, login, password, address, date_of_birth, analyse_type, doc_full_name, doc_phone, lab_image) =>
        Patient(
          created_at.toLocalDateTime,
          firstname = firstname,
          lastname = lastname,
          phone = phone,
          customer_id = customer_id,
          company_code = company_code,
          login = login,
          password = password,
          address = address,
          dateOfBirth = date_of_birth.toLocalDateTime,
          analyseType = analyse_type,
          docFullName = doc_full_name,
          docPhone = doc_phone,
          analysis_image_name = lab_image
        )
    }
  implicit val writePatient: Write[Patient] =
    Write[(Timestamp, String, String, String, String, String, String, String, String, Timestamp, String, Option[String])].contramap { f =>
        (
          javaLdTime2JavaSqlTimestamp(f.created_at),
          f.firstname,
          f.lastname,
          f.phone,
          f.customer_id,
          f.company_code,
          f.login,
          f.password,
          f.address,
          javaLdTime2JavaSqlTimestamp(f.dateOfBirth),
          f.analyseType,
          f.docFullName
        )
    }
  implicit val userRead: Read[User] =
    Read[(Timestamp, String, String, String, String, String, String,String)].map {
      case (created_at, firstname, lastname, phone, role, company_code, login, password) =>
        User(created_at.toLocalDateTime, firstname, lastname, phone, role, company_code, login, password)
    }
  implicit val statsActionRead: Read[StatsAction] =
    Read[(Timestamp, String, String, String, String, String)].map {
      case (created_at, company_code, action, login, ip_address, user_agent) =>
        StatsAction(created_at.toLocalDateTime, company_code, action, login, ip_address, user_agent)
    }

  private def javaLdTime2JavaSqlTimestamp(ldTime: LocalDateTime): Timestamp = {
    Timestamp.valueOf(ldTime)
  }

  private def updateQueryWithUniqueId(fr: Fragment): doobie.ConnectionIO[Int] = {
    fr.update.withUniqueGeneratedKeys[Int]("id")
  }

  def create(patient: Patient): doobie.ConnectionIO[Int] = {
    val values = {
      fr""" values (
        ${javaLdTime2JavaSqlTimestamp(patient.created_at)},${patient.firstname}, ${patient.lastname},
        ${patient.phone}, ${patient.customer_id}, ${patient.company_code}, ${patient.login}, ${patient.password},
        ${patient.address}, ${javaLdTime2JavaSqlTimestamp(patient.dateOfBirth)}, ${patient.analyseType}
      )"""
    }

    val fieldsName =
      fr"""
        insert into "Patients" (created_at, firstname, lastname, phone, customer_id, company_code, login, password,
         address, date_of_birth, analyse_type)
        """

    updateQueryWithUniqueId(fieldsName ++ values)
  }

  def addDeliveryStatus(customerId: String, deliveryStatus: String): Update0 = {
    sql"""UPDATE "Patients" SET delivery_status=$deliveryStatus
          WHERE customer_id=$customerId""".update
  }

  def addStatsAction(statsAction: StatsAction): doobie.ConnectionIO[Int] = {
    val values = fr"(${javaLdTime2JavaSqlTimestamp(statsAction.created_at)},${statsAction.company_code}, ${statsAction.action}, ${statsAction.login}, ${statsAction.ip_address}, ${statsAction.user_agent})"

    sql"""INSERT INTO "Stats" (created_at, company_code, action, login, ip_address, user_agent)
          VALUES $values""".update.withUniqueGeneratedKeys[Int]("id")
  }

  def createUser(user: User): doobie.ConnectionIO[Int] = {
    val values = fr"(${javaLdTime2JavaSqlTimestamp(user.created_at)},${user.firstname}, ${user.lastname}, ${user.phone}, ${user.role}, ${user.company_code}, ${user.login}, ${user.password})"

    sql"""insert into "Users" (created_at, firstname, lastname, phone, role, company_code, login, password)
          values $values""".update.withUniqueGeneratedKeys[Int]("id")
  }

  def addAnalysisResult(customerId: String, analysisFileName: String): Update0 = {
    sql"""UPDATE "Patients" SET analysis_image_name=$analysisFileName
          WHERE customer_id=$customerId""".update
  }

  def getByCustomerId(customerId: String): Query0[Patient] = {
    val querySql = fr"""SELECT created_at,firstname,lastname,phone,customer_id,company_code,login,password,analysis_image_name FROM "Patients" WHERE customer_id = $customerId"""
    querySql.query[Patient]
  }

  def getPatientByLogin(login: String): doobie.Query0[Patient] = {
    val querySql = fr"""select created_at,firstname,lastname,phone,customer_id, company_code,login,password,analysis_image_name from "Patients" WHERE login = $login"""
      querySql.query[Patient]
  }

  def getUserByLogin(login: String): doobie.Query0[User] = {
    val querySql = fr"""select created_at,firstname,lastname,phone,role,company_code,login,password from "Users" WHERE login = $login"""
      querySql.query[User]
  }

  def getPatients: ConnectionIO[List[Patient]] = {
    val querySql =
      sql"""
        SELECT created_at,firstname,lastname,phone,customer_id,company_code,login,password,address,date_of_birth,doc_full_name,doc_phone,analysis_image_name FROM "Patients" ORDER BY created_at
      """
    querySql.query[Patient].to[List]
  }

  def getStats: ConnectionIO[List[StatsAction]] = {
    val querySql = fr"""SELECT created_at, company_code, action, ip_address, user_agent FROM "Stats" ORDER BY created_at """
    querySql.query[StatsAction].to[List]
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