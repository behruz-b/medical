package doobie.domain

import protocols.DoobieProtocol.DoobieTest

trait CreatorRepositoryAlgebra[F[_]] {

  def create(name: String, login: String): F[Int]

}

trait ChatRepositoryAlgebra[F[_]] extends CreatorRepositoryAlgebra[F]
