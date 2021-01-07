package doobie.domain

import protocols.AppProtocol.Patient

trait PatientRepositoryAlgebra[F[_]] {

  def create(patient: Patient): F[Int]
  def addAnalysisResult(customerId: String, analysisFileName: String): F[Int]
  def getByCustomerId(customerId: String): fs2.Stream[F,Patient]
  def getPatientByLogin(login: String):fs2.Stream[F,Patient]
  def getPatients: F[List[Patient]]

}

trait ChatRepositoryAlgebra[F[_]] extends PatientRepositoryAlgebra[F]
