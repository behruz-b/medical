package doobie.domain

import protocols.PatientProtocol._
import protocols.UserProtocol.User

trait PatientRepositoryAlgebra[F[_]] {

  def create(patient: Patient): F[Int]
  def createUser(user: User): F[Int]
  def addStatsAction(statsAction: StatsAction): F[Int]
  def addAnalysisResult(customerId: String, analysisFileName: String): F[Int]
  def getByCustomerId(customerId: String): fs2.Stream[F,Patient]
  def getPatientByLogin(login: String):fs2.Stream[F,Patient]
  def getUserByLogin(login: String):fs2.Stream[F,User]
  def getPatients: F[List[Patient]]
  def getStats: F[List[StatsAction]]

}

trait ChatRepositoryAlgebra[F[_]] extends PatientRepositoryAlgebra[F]
