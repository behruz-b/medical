package doobie.domain

import protocols.AppProtocol.Paging.{PageReq, PageRes}
import protocols.PatientProtocol._
import protocols.UserProtocol.{Roles, User}

import java.time.LocalDateTime

trait PatientRepositoryAlgebra[F[_]] {

  def create(patient: Patient): F[Int]

  def createUser(user: User): F[Int]

  def addStatsAction(statsAction: StatsAction): F[Int]

  def addPatientsDoc(patientsDoc: PatientsDoc): F[Int]

  def addAnalysisResult(analysisFileName: String, created_at: LocalDateTime, customerId: String): F[Int]

  def changePassword(login: String, newPass: String): F[Int]

  def addDeliveryStatus(customerId: String, deliveryStatus: String): F[Int]

  def addSmsLinkClick(customerId: String, smsLinkClick: String): F[Int]

  def searchByPatientName(firstname: String): F[List[Patient]]

  def getByCustomerId(customerId: String): fs2.Stream[F, Patient]

  def getAnalysisResultsByCustomerId(customerId: String): F[List[PatientAnalysisResult]]

  def getPatientByLogin(login: String): fs2.Stream[F, Patient]

  def getUserByLogin(login: String): fs2.Stream[F, User]

  def getPatients(analyseType: String,
                  startDate: Option[LocalDateTime] = None,
                  endDate: Option[LocalDateTime] = None,
                  pageReq: PageReq): F[PageRes[Patient]]

  def getStats: F[List[StatsAction]]

  def getPatientsDoc: F[List[GetPatientsDocById]]

  def getPatientsTable: F[List[(LocalDateTime, String, Option[String])]]

  def getRoles: F[List[Roles]]

}

trait ChatRepositoryAlgebra[F[_]] extends PatientRepositoryAlgebra[F]
