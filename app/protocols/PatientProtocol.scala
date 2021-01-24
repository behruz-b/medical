package protocols

import java.time.{LocalDate, LocalDateTime}
import java.util.Date
import play.api.libs.json._


object PatientProtocol {

  case class Patient(created_at: LocalDateTime,
                     firstname: String,
                     lastname: String,
                     phone: String,
                     customer_id: String,
                     company_code: String,
                     login: String,
                     password: String,
                     address: String,
                     dateOfBirth: LocalDate,
                     analyseType: String,
                     docFullName: Option[String] = None,
                     docPhone: Option[String] = None,
                     analysis_image_name: Option[String] = None) {
    def id: Option[Int] = None
  }
  implicit val patientFormat: OFormat[Patient] = Json.format[Patient]

  case class StatsAction(created_at: LocalDateTime,
                         company_code: String,
                         action: String,
                         ip_address: String,
                         login: String,
                         user_agent: String)

  implicit val StatsActionFormat: OFormat[StatsAction] = Json.format[StatsAction]

  case class CreatePatient(patient: Patient)
  case class AddStatsAction(statsAction: StatsAction)
  case class AddAnalysisResult(customerId: String, analysisFileName: String)
  case class GetPatientByCustomerId(customerId: String)
  case class GetPatientByLogin(login: String, password: String)
  case object GetPatients
  case object GetStats
  case class SendSmsToCustomer(customerId: String)
  case class CheckSmsDeliveryStatus(requestId: String, customerId: String)

  val analysisType = List(
    "Bosh miya",
    "Bosh miya, Ko'z, Quloq, Burun bo'shliqlari, Ginofiz, Qon tomirlari",
    "Bo'yin umurtqalari, Orqa miya",
    "Bo'yin umurtqalari, Orqa miya, Qon tomirlari",
    "Ko'krak umurtqalari, Orqa miya",
    "Bel umurtqalari, Orqa miya",
    "Qorin bo'shligi, Jigar, Taloq, O't puffagi qo'llari, Oshqozon osti bezi, Buyraklar",
    "Buyrak usti bezlari, Tizza",
    "Qo'l oyoq bo'g'imlari",
    "Chanoq son bo'g'imlari, Tizza",
    "Ayollar kichik chanoq a'zolari",
    "Erkaklar kichik chanoq a'zolari",
    "Bo'yin yumshoq to'qimalari",
    "Tana a'zolarini umumiy MRT tekshiruvi",
    "MRT kontrasi 'Magnilik'"
  )
  val roleTypes = List("doc", "reg")

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
    s"Elegant Farm\\nAnaliz natijasini olish uchun xavolani bosing:\\nhttp://elegant-farm.uz/r/$customerId"
}
