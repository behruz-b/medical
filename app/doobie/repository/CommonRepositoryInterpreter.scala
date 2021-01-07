package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.PatientRepositoryAlgebra
import doobie.implicits._
import protocols.AppProtocol.Patient

trait CommonSQL {

  def create(patient: Patient): ConnectionIO[Int]
  def addAnalysisResult(customerId: String, analysisFileName: String): Update0
  def getByCustomerId(customerId: String): Query0[Patient]
  def getPatientByLogin(login: String): Query0[Patient]
  def getPatients: ConnectionIO[List[Patient]]

}

abstract class CommonRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends PatientRepositoryAlgebra[F] {

  val commonSql: CommonSQL

  override def create(patient: Patient): F[Int] = {
    commonSql.create(patient).transact(xa)
  }
  override def addAnalysisResult(customerId: String, analysisFileName: String): F[Int] = {
    commonSql.addAnalysisResult(customerId, analysisFileName).run.transact(xa)
  }
  override def getByCustomerId(customerId: String): fs2.Stream[F,Patient] = {
    commonSql.getByCustomerId(customerId).stream.transact(xa)
  }
  override def getPatientByLogin(login: String): fs2.Stream[F,Patient] = {
    commonSql.getPatientByLogin(login).stream.transact(xa)
  }

  override def getPatients: F[List[Patient]] = {
    commonSql.getPatients.transact(xa)
  }

}