package doobie.domain

import protocols.AppProtocol.Paging.{PageReq, PageRes}
import protocols.PatientProtocol._
import protocols.UserProtocol.{Roles, User}

trait PatientRepositoryAlgebra[F[_]] {

  def create(patient: Patient): F[Int]
  def createUser(user: User): F[Int]
  def addStatsAction(statsAction: StatsAction): F[Int]
  def addPatientsDoc(patientsDoc: PatientsDoc): F[Int]
  def addAnalysisResult(customerId: String, analysisFileName: String): F[Int]
  def changePassword(login: String, newPass: String): F[Int]
  def addDeliveryStatus(customerId: String, deliveryStatus: String): F[Int]
  def addSmsLinkClick(customerId: String, smsLinkClick: String): F[Int]
  def getByCustomerId(customerId: String): fs2.Stream[F,Patient]
  def getPatientByLogin(login: String):fs2.Stream[F,Patient]
  def getUserByLogin(login: String):fs2.Stream[F,User]
  def getPatients(analyseType: String, pageReq: PageReq): F[PageRes[Patient]]
  def getStats: F[List[StatsAction]]
  def getPatientsDoc: F[List[GetPatientsDocById]]
  def getRoles: F[List[Roles]]

}

trait ChatRepositoryAlgebra[F[_]] extends PatientRepositoryAlgebra[F]
