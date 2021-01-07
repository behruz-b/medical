package protocols

import java.time.LocalDateTime

import play.api.libs.json._


object AppProtocol {

  case class Patient(created_at: LocalDateTime,
                     firstname: String,
                     lastname: String,
                     phone: String,
                     email: Option[String] = None,
                     passport: String,
                     customer_id: String,
                     login: String,
                     password: String,
                     analysis_image_name: Option[String] = None) {
    def id: Option[Int] = None
  }
  implicit val patientFormat = Json.format[Patient]

  case class CreatePatient(patient: Patient)
  case class AddAnalysisResult(customerId: String, analysisFileName: String)
  case class GetPatientByCustomerId(customerId: String)
  case class GetPatientByLogin(login: String, password: String)
  case object GetPatients


}
