package doobie.domain

import protocols.AppProtocol.Patient

trait CreatorRepositoryAlgebra[F[_]] {

  def create(patient: Patient): F[Int]

}

trait ChatRepositoryAlgebra[F[_]] extends CreatorRepositoryAlgebra[F]
