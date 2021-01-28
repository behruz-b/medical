package protocols

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}


object PatientProtocol {
  implicit def localDateFormat(fieldName: String, dateFormat: String = "dd/MM/yyyy"): Reads[LocalDate] =
    (__ \ fieldName).read[String].map(s => LocalDate.parse(s, DateTimeFormatter.ofPattern(dateFormat)))

  case class PatientForm(firstName: String,
                         lastName: String,
                         phone: String,
                         dateOfBirth: LocalDate,
                         address: String,
                         analyseType: String,
                         analyseGroup: String,
                         docFullName: Option[String] = None,
                         docPhone: Option[String] = None,
                         company_code: String)


  implicit val patientFormReads: Reads[PatientForm] = (
      (__ \ "firstName").read[String] and
      (__ \ "lastName").read[String] and
      (__ \ "phone").read[String] and
      localDateFormat("dateOfBirth") and
      (__ \ "address").read[String] and
      (__ \ "analyseType").read[String] and
      (__ \ "analyseGroup").read[String] and
      (__ \ "docFullName").formatNullable[String] and
      (__ \ "docPhone").formatNullable[String] and
      (__ \ "company_code").read[String]
    )(PatientForm)

  case class DoctorForm(firstName: String,
                        lastName: String,
                        phone: String,
                        role: String,
                        login: String,
                        company_code: String)

  implicit val doctorFormReads: Reads[DoctorForm] = (
    (__ \ "firstName").read[String] and
      (__ \ "lastName").read[String] and
      (__ \ "phone").read[String] and
      (__ \ "role").read[String] and
      (__ \ "login").read[String] and
      (__ \ "company_code").read[String]
    )(DoctorForm)

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
                     analyseGroup: String,
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
    "MRT",
    "MSKT",
    "UZI",
    "Lobaratoriya"
  )

  val mrtType = List(
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

  val msktType = List(
    "Bosh miya, Ko'z, Quloq, Burun bo'shliqlari",
    "Bo'yin umurtqalari",
    "Ko'krak umurtqalari",
    "Ko'krak qafasi",
    "Bel umurtqalari",
    "Buyrak usti bezlari",
    "Qo'l oyoq bo'g'imlari",
    "Chanoq son bo'g'inlari",
    "Ayollar kichik chanoq a'zolari",
    "Erkaklar kichik chanoq a'zolari",
    "Qorin bo'shligi, Jigar, Taloq, O't pufagi qo'llari, Oshqozon osti bezi, Buyraklar",
    "Buyraklar, Siydik yo'llari, Siydik pufagi",
    "Kuks orligi a'zolari (Qizil ungach)"
  )

  /**
   *
   * ==Overview==
   *
   * Case class SmsStatus description.
   * {{{
   *  code: Int,
   *  delivered-date: Date, Format(Y-m-d H:i:s),
   *  description: String,
   *  message-count: Int,
   *  ordinal: Int
   * }}}
   */

  val SmsText: String => String = (customerId: String) =>
    s"Sizga katta rahmat bizning 'Elegant Farm' Diagnostika Markaziga tashrif buyurganingiz uchun, sizning" +
      s" sog'liqingiz biz uchun juda muhim.\\nTibbiy xulosangizni quyidagi havola orqali olishingiz mumkin:" +
      s"\\nhttp://elegant-farm.uz/r/$customerId"
}
