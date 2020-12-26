package doobie.repository

import cats.effect.Bracket
import doobie._
import doobie.domain.CreatorRepositoryAlgebra
import doobie.implicits.toSqlInterpolator

object MessageSQL extends CommonSQL  {

  implicit val han = LogHandler.jdkLogHandler

  def create(name: String, login: String): doobie.ConnectionIO[Int] = {
    sql"""insert into "Doobie" (nameEncr,loginEncr) values ($name, $login)""".update.withUniqueGeneratedKeys[Int]("id")
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