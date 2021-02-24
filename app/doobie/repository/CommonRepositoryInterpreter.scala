package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.PatientRepositoryAlgebra
import doobie.implicits._
import protocols.AppProtocol.Paging.{PageReq, PageRes}
import protocols.PatientProtocol._
import protocols.UserProtocol.{Roles, User}

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime}

trait CommonSQL {

  def create(patient: Patient): ConnectionIO[Int]
  def createUser(user: User): ConnectionIO[Int]
  def addStatsAction(statsAction: StatsAction): ConnectionIO[Int]
  def addPatientsDoc(patientsDoc: PatientsDoc): ConnectionIO[Int]
  def addAnalysisResult(analysisFileName: String, created_at: LocalDateTime, customerId: String): ConnectionIO[Int]
  def changePassword(login: String, newPass: String): Update0
  def addDeliveryStatus(customerId: String, deliveryStatus: String): Update0
  def addSmsLinkClick(customerId: String, smsLinkClick: String): Update0
  def searchByPatientName(firstname: String): ConnectionIO[List[Patient]]
  def getByCustomerId(customerId: String): Query0[Patient]
  def getAnalysisResultsByCustomerId(customerId: String): Query0[PatientAnalysisResult]
  def getPatientByLogin(login: String): Query0[Patient]
  def getUserByLogin(login: String): Query0[User]
  def getPatients(analyseType: String,
                  startDate: Option[LocalDateTime],
                  endDate: Option[LocalDateTime],
                  pageReq: PageReq): ConnectionIO[PageRes[Patient]]
  def getStats: ConnectionIO[List[StatsAction]]
  def getPatientsDoc: ConnectionIO[List[GetPatientsDocById]]
  def getPatientsTable: ConnectionIO[List[(LocalDateTime, String, Option[String])]]
  def getRoles: ConnectionIO[List[Roles]]

}

abstract class CommonRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends PatientRepositoryAlgebra[F] {

  val commonSql: CommonSQL

  override def create(patient: Patient): F[Int] = {
    commonSql.create(patient).transact(xa)
  }
  override def createUser(user: User): F[Int] = {
    commonSql.createUser(user).transact(xa)
  }
  override def addStatsAction(statsAction: StatsAction): F[Int] = {
    commonSql.addStatsAction(statsAction).transact(xa)
  }
  override def addPatientsDoc(patientsDoc: PatientsDoc): F[Int] = {
    commonSql.addPatientsDoc(patientsDoc).transact(xa)
  }
  override def addAnalysisResult(analysisFileName: String, created_at: LocalDateTime, customerId: String): F[Int] = {
    commonSql.addAnalysisResult(analysisFileName, created_at, customerId).transact(xa)
  }
  override def changePassword(login: String, newPass: String): F[Int] = {
    commonSql.changePassword(login, newPass).run.transact(xa)
  }
  override def addDeliveryStatus(customerId: String, deliveryStatus: String): F[Int] = {
    commonSql.addDeliveryStatus(customerId, deliveryStatus).run.transact(xa)
  }
  override def addSmsLinkClick(customerId: String, smsLinkClick: String): F[Int] = {
    commonSql.addSmsLinkClick(customerId, smsLinkClick).run.transact(xa)
  }
  override def getByCustomerId(customerId: String): fs2.Stream[F,Patient] = {
    commonSql.getByCustomerId(customerId).stream.transact(xa)
  }
  override def getAnalysisResultsByCustomerId(customerId: String): fs2.Stream[F,PatientAnalysisResult] = {
    commonSql.getAnalysisResultsByCustomerId(customerId).stream.transact(xa)
  }
  override def searchByPatientName(firstName: String): F[List[Patient]] = {
    commonSql.searchByPatientName(firstName).transact(xa)
  }
  override def getPatientByLogin(login: String): fs2.Stream[F,Patient] = {
    commonSql.getPatientByLogin(login).stream.transact(xa)
  }
  override def getUserByLogin(login: String): fs2.Stream[F,User] = {
    commonSql.getUserByLogin(login).stream.transact(xa)
  }
  override def getPatients(analyseType: String,
                           startDate: Option[LocalDateTime],
                           endDate: Option[LocalDateTime],
                           pageReq: PageReq): F[PageRes[Patient]] = {
    commonSql.getPatients(analyseType, startDate, endDate,  pageReq).transact(xa)
  }
  override def getStats: F[List[StatsAction]] = {
    commonSql.getStats.transact(xa)
  }
  override def getPatientsDoc: F[List[GetPatientsDocById]] = {
    commonSql.getPatientsDoc.transact(xa)
  }
  override def getPatientsTable: F[List[(LocalDateTime, String, Option[String])]] = {
    commonSql.getPatientsTable.transact(xa)
  }
  override def getRoles: F[List[Roles]] = {
    commonSql.getRoles.transact(xa)
  }

}