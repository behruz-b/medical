package protocols

import cats.implicits.{catsSyntaxOptionId, none}
import org.apache.commons.lang3.StringUtils
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import protocols.AppProtocol.Paging.PageReq

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}


object PatientProtocol {
  implicit def localDateFormat(fieldName: String, dateFormat: String = "dd/MM/yyyy"): Reads[LocalDate] =
    (__ \ fieldName).read[String].map(s => LocalDate.parse(s, DateTimeFormatter.ofPattern(dateFormat)))

  implicit def optLdtRead(fieldName: String, dateFormat: String = "yyyy-MM-dd HH:mm:ss"): Reads[Option[LocalDateTime]] = {
    (__ \ fieldName).readNullable[String] map {
      case Some(s) if StringUtils.isNotBlank(s) => LocalDateTime.parse(s, DateTimeFormatter.ofPattern(dateFormat)).some
      case _ => None
    }
  }

  def optStringRead(fieldName: String): Reads[Option[String]] =
    (__ \ fieldName).readNullable[String].map { s =>
      if (s.exists(StringUtils.isNotBlank)) s else None
    }

  implicit def optIntRead(fieldName: String): Reads[Option[Int]] =
    (__ \ fieldName).readNullable[Int].map { s =>
      if (s.isDefined) s else None
    }

  case class PatientForm(firstName: String,
                         lastName: String,
                         phone: String,
                         dateOfBirth: LocalDate,
                         address: String,
                         analyseType: String,
                         analyseGroup: String,
                         docFullName: Option[String] = None,
                         docPhone: Option[String] = None,
                         companyCode: String,
                         docId: Option[Int] = None)

  implicit val patientFormReads: Reads[PatientForm] = (
      (__ \ "firstName").read[String] and
      (__ \ "lastName").read[String] and
      (__ \ "phone").read[String] and
      localDateFormat("dateOfBirth") and
      (__ \ "address").read[String] and
      (__ \ "analysisType").read[String] and
      (__ \ "analysisGroup").read[String] and
      optStringRead("docFullName") and
      optStringRead("docPhone") and
      (__ \ "companyCode").read[String] and
      optIntRead("docId")
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
    ) (DoctorForm)

  case class PatientsDocForm(fullName: String,
                        phone: String)

  implicit val patientsDocFormReads: Reads[PatientsDocForm] = (
      (__ \ "fullName").read[String] and
      (__ \ "phone").read[String]
    ) (PatientsDocForm)

  case class PatientsReport(startDate: Option[LocalDateTime] = None,
                            endDate: Option[LocalDateTime] = None,
                            analyseType: String = "MRT")

  implicit val PatientsReportReads: Reads[PatientsReport] = (
    optLdtRead("startDate") and
      optLdtRead("endDate") and
      (__ \ "analyseType").read[String]
    ) (PatientsReport)

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
                     smsLinkClick: Option[String] = None,
                     analysis_image_name: Option[String] = None,
                     docId: Option[Int] = None) {
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

  case class PatientsDoc(fullname: String,
                         phone: String)

  implicit val PatientsDocFormat: OFormat[PatientsDoc] = Json.format[PatientsDoc]

  case class GetPatientsDocById(id: Int, fullname: String, phone: String)
  implicit val formatPatientsDocByIdFormat: OFormat[GetPatientsDocById] = Json.format[GetPatientsDocById]

  case class CreatePatient(patient: Patient)

  case class AddStatsAction(statsAction: StatsAction)

  case class AddPatientsDoc(patientsDoc: PatientsDoc)

  case class PatientAnalysisResult(analysisFileName: String,
                               created_at: LocalDateTime,
                               customerId: String)
  implicit val formatPatientAnalysisResults: OFormat[PatientAnalysisResult] = Json.format[PatientAnalysisResult]

  case class AddSmsLinkClick(customerId: String, smsLinkClick: String)

  case class GetPatientByCustomerId(customerId: String)

  case class GetAnalysisResultsByCustomerId(customerId: String)

  case class GetPatientByLogin(login: String, password: String)

  case class GetPatients(analyseType: String = "MRT",
                         dateRangeStart: Option[LocalDateTime],
                         dateRangeEnd: Option[LocalDateTime],
                         pageReq: PageReq)

  case object GetStats

  case object GetPatientsDoc

  case class SendSmsToCustomer(customerId: String)

  case class CheckCustomerId(customerId: String)

  case class SendIdToPatientViaSms(customerId: String)

  case class CheckSmsDeliveryStatus(requestId: String, customerId: String)

  case class SearchByPatientName(firstname: String)

  val analysisType = List(
    "MRT",
    "MSKT",
    "UZI",
    "Laboratoriya"
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
  val uziType = List(
    "Осмотр печени ж/п селезенки",
    "Поджелудочные железы",
    "Осмотр мочевого пузырья, придаткой матки в.т.ч",
    "Осмотр почек",
    "Осмотр простаты",
    "Осмотр щитовидной железы",
    "Осмотр плевральной полости",
    "УЗИ общее(печень, ж/п, селезенка, матка, яичники, м/п, почки)",
    "УЗИ общее(печень, ж/п, селезенка, м/п, простата, почки)",
    "Вагинальные исследования",
    "Узи беременность",
    "КУКС"
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
  val laboratoryType = List(
    "Общий анализ крови ОАК",
    "Время свертываемости крови ВСК",
    "Протромбиновое время ПТИ МНО",
    "АЧТВ",
    "Протромбиновое время, соотношение",
    "ПТН",
    "Протромбиновое время",
    "HBsAg (Гепатит В)",
    "HCV (Гепатит С)",
    "Антистрептолизин-О (АСЛО)",
    "С-реактивный белок (СРБ)",
    "Ревматоидный фактор (РФ)",
    "Общий белок",
    "Билирубин (общий, прямой, непрямой)",
    "Аланиноминотрансфераза (АЛТ)",
    "Аспартатаминотрансфераза (АСТ)",
    "Креатинин",
    "Мочевина"
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

  val SmsTextForPatientId: String => String = (customerId: String) =>
    s"'Elegant Farm' Diagnostika Markazi\\nSiz ro'yxatdan o'tdingiz sizning ID: $customerId"
}
