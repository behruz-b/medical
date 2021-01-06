package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.PatientRepositoryAlgebra
import doobie.implicits._
import protocols.AppProtocol.Patient

trait CommonSQL {

  def create(patient: Patient): ConnectionIO[Int]
  def labResult(patient: Patient): ConnectionIO[Int]
  def getByCustomerId(customerId: String): Query0[Patient]
  def getPatientByLogin(login: String): Query0[Patient]

}

abstract class CommonRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends PatientRepositoryAlgebra[F] {

  val commonSql: CommonSQL

  override def create(patient: Patient): F[Int] = {
    commonSql.create(patient).transact(xa)
  }
  override def labResult(patient: Patient): F[Int] = {
    commonSql.labResult(patient).transact(xa)
  }
  override def getByCustomerId(customerId: String): fs2.Stream[F,Patient] = {
    commonSql.getByCustomerId(customerId).stream.transact(xa)
  }
  def getPatientByLogin(login: String): fs2.Stream[F,Patient] = {
    commonSql.getPatientByLogin(login).stream.transact(xa)
  }

}