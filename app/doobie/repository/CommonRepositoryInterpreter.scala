package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.CreatorRepositoryAlgebra
import doobie.implicits._
import protocols.AppProtocol.Patient

trait CommonSQL {


  def create(patient: Patient): ConnectionIO[Int]

}

abstract class CommonRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends CreatorRepositoryAlgebra[F] {

  val commonSql: CommonSQL

  override def create(patient: Patient): F[Int] = {
    commonSql.create(patient).transact(xa)
  }

}