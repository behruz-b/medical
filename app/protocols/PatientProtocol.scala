package protocols

import java.time.LocalDateTime

import play.api.libs.json._


object PatientProtocol {

  case class Patient(created_at: LocalDateTime,
                     firstname: String,
                     lastname: String,
                     phone: String,
                     email: Option[String] = None,
                     passport: String,
                     customer_id: String,
                     company_code: String,
                     login: String,
                     password: String,
                     analysis_image_name: Option[String] = None) {
    def id: Option[Int] = None
  }
  implicit val patientFormat: OFormat[Patient] = Json.format[Patient]

  case class CreatePatient(patient: Patient)
  case class AddAnalysisResult(customerId: String, analysisFileName: String)
  case class GetPatientByCustomerId(customerId: String)
  case class GetPatientByLogin(login: String, password: String)
  case object GetPatients
  case class SendSmsToCustomer(customerId: String)

  /**
   *
   * ==Overview==
   *
   * Case class SmsStatus description.
   *  {{{
   *  code: Int,
   *  delivered-date: Date, Format(Y-m-d H:i:s),
   *  description: String,
   *  message-count: Int,
   *  ordinal: Int
   *  }}}
   */

  val SmsText: String => String = (customerId: String) =>
    s"Tahlil natijasini kuyidagi xavola orqali olishingiz mumkin:" +
      s"http://localhost:9000/analysis-result/$customerId"
}
