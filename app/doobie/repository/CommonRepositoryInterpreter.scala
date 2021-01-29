package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.PatientRepositoryAlgebra
import doobie.implicits._
import protocols.PatientProtocol._
import protocols.UserProtocol.{Roles, User}

trait CommonSQL {

  def create(patient: Patient): ConnectionIO[Int]
  def createUser(user: User): ConnectionIO[Int]
  def addStatsAction(statsAction: StatsAction): ConnectionIO[Int]
  def addAnalysisResult(customerId: String, analysisFileName: String): Update0
  def addDeliveryStatus(customerId: String, deliveryStatus: String): Update0
  def addSmsLinkClick(customerId: String, smsLinkClick: String): Update0
  def getByCustomerId(customerId: String): Query0[Patient]
  def getPatientByLogin(login: String): Query0[Patient]
  def getUserByLogin(login: String): Query0[User]
  def getPatients: ConnectionIO[List[Patient]]
  def getStats: ConnectionIO[List[StatsAction]]
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
  override def addAnalysisResult(customerId: String, analysisFileName: String): F[Int] = {
    commonSql.addAnalysisResult(customerId, analysisFileName).run.transact(xa)
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
  override def getPatientByLogin(login: String): fs2.Stream[F,Patient] = {
    commonSql.getPatientByLogin(login).stream.transact(xa)
  }
  override def getUserByLogin(login: String): fs2.Stream[F,User] = {
    commonSql.getUserByLogin(login).stream.transact(xa)
  }
  override def getPatients: F[List[Patient]] = {
    commonSql.getPatients.transact(xa)
  }
  override def getStats: F[List[StatsAction]] = {
    commonSql.getStats.transact(xa)
  }
  override def getRoles: F[List[Roles]] = {
    commonSql.getRoles.transact(xa)
  }

}