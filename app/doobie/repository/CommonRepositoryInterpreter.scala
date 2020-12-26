package doobie.repository

import cats.effect.Bracket
import doobie.domain.CreatorRepositoryAlgebra
import doobie._
import cats.implicits._
import doobie.implicits._

trait CommonSQL {


  def create(name: String, login: String): ConnectionIO[Int]

}

abstract class CommonRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends CreatorRepositoryAlgebra[F] {

  val commonSql: CommonSQL

  override def create(name: String, login: String): F[Int] = {
    commonSql.create(name, login).transact(xa)
  }

}