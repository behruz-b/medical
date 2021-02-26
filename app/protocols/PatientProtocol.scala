package protocols

import cats.implicits.catsSyntaxOptionId
import org.apache.commons.lang3.StringUtils
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
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

  sealed trait AnalysisTypeBox
  sealed case class AnalysisType(name: String, code: String) extends AnalysisTypeBox
  object MRT extends AnalysisType("MRT", "mrt.type")
  object MSKT extends AnalysisType("MSKT", "mskt.type")
  object UZI extends AnalysisType("UZI", "uzi.type")
  object Laboratoriya extends AnalysisType("Laboratoriya", "laboratoriya.type")

  sealed case class AnalysisGroup(name: String, code: String, analysisType: AnalysisType) extends AnalysisTypeBox
  object MRT1 extends AnalysisGroup("Bosh miya", "mrt1", MRT)
  object MRT2 extends AnalysisGroup("Bosh miya, Ko'z, Quloq, Burun bo'shliqlari, Ginofiz, Qon tomirlari", "mrt2", MRT)
  object MRT3 extends AnalysisGroup("Bo'yin umurtqalari, Orqa miya", "mrt3", MRT)
  object MRT4 extends AnalysisGroup("Bo'yin umurtqalari, Orqa miya, Qon tomirlari", "mrt4", MRT)
  object MRT5 extends AnalysisGroup("Ko'krak umurtqalari, Orqa miya", "mrt5", MRT)
  object MRT6 extends AnalysisGroup("Bel umurtqalari, Orqa miya", "mrt6", MRT)
  object MRT7 extends AnalysisGroup("Qorin bo'shligi, Jigar, Taloq, O't puffagi qo'llari, Oshqozon osti bezi, Buyraklar", "mrt7", MRT)
  object MRT8 extends AnalysisGroup("Buyrak usti bezlari, Tizza", "mrt8", MRT)
  object MRT9 extends AnalysisGroup("Qo'l oyoq bo'g'imlari", "mrt9", MRT)
  object MRT10 extends AnalysisGroup("Chanoq son bo'g'imlari, Tizza", "mrt10", MRT)
  object MRT11 extends AnalysisGroup("Ayollar kichik chanoq a'zolari", "mrt11", MRT)
  object MRT12 extends AnalysisGroup("Erkaklar kichik chanoq a'zolari", "mrt12", MRT)
  object MRT13 extends AnalysisGroup("Bo'yin yumshoq to'qimalari", "mrt13", MRT)
  object MRT14 extends AnalysisGroup("Tana a'zolarini umumiy MRT tekshiruvi", "mrt14", MRT)
  object MRT15 extends AnalysisGroup("MRT kontrasi 'Magnilik", "mrt15", MRT)

  object MSKT1 extends AnalysisGroup("Bosh miya, Ko'z, Quloq, Burun bo'shliqlari", "mskt1", MSKT)
  object MSKT2 extends AnalysisGroup("Bo'yin umurtqalari", "mskt2", MSKT)
  object MSKT3 extends AnalysisGroup("Ko'krak umurtqalari", "mskt3", MSKT)
  object MSKT4 extends AnalysisGroup("Ko'krak qafasi", "mskt4", MSKT)
  object MSKT5 extends AnalysisGroup("Bel umurtqalari", "mskt5", MSKT)
  object MSKT6 extends AnalysisGroup("Buyrak usti bezlari", "mskt6", MSKT)
  object MSKT7 extends AnalysisGroup("Qo'l oyoq bo'g'imlari", "mskt7", MSKT)
  object MSKT8 extends AnalysisGroup("Chanoq son bo'g'inlari", "mskt8", MSKT)
  object MSKT9 extends AnalysisGroup("Ayollar kichik chanoq a'zolari", "mskt9", MSKT)
  object MSKT10 extends AnalysisGroup("Erkaklar kichik chanoq a'zolari", "mskt10", MSKT)
  object MSKT11 extends AnalysisGroup("Qorin bo'shligi, Jigar, Taloq, O't pufagi qo'llari, Oshqozon osti bezi, Buyraklar", "mskt11", MSKT)
  object MSKT12 extends AnalysisGroup("Buyraklar, Siydik yo'llari, Siydik pufagi", "mskt12", MSKT)
  object MSKT13 extends AnalysisGroup("Kuks orligi a'zolari (Qizil ungach)", "mskt13", MSKT)


  object UZI1 extends AnalysisGroup("Осмотр печени ж/п селезенки", "uzi1", MSKT)
  object UZI2 extends AnalysisGroup("Поджелудочные железы", "uzi2", MSKT)
  object UZI3 extends AnalysisGroup("Осмотр мочевого пузырья, придаткой матки в.т.ч", "uzi3", MSKT)
  object UZI4 extends AnalysisGroup("Осмотр почек", "uzi4", MSKT)
  object UZI5 extends AnalysisGroup("Осмотр простаты", "uzi5", MSKT)
  object UZI6 extends AnalysisGroup("Осмотр щитовидной железы", "uzi6", MSKT)
  object UZI7 extends AnalysisGroup("Осмотр плевральной полости", "uzi7", MSKT)
  object UZI8 extends AnalysisGroup("УЗИ общее(печень, ж/п, селезенка, матка, яичники, м/п, почки)", "uzi8", MSKT)
  object UZI9 extends AnalysisGroup("УЗИ общее(печень, ж/п, селезенка, м/п, простата, почки)", "uzi9", MSKT)
  object UZI10 extends AnalysisGroup("Вагинальные исследования", "uzi10", MSKT)
  object UZI11 extends AnalysisGroup("Узи беременность", "uzi11", MSKT)
  object UZI12 extends AnalysisGroup("КУКС", "uzi12", MSKT)

  object LAB1 extends AnalysisGroup("Общий анализ крови ОАК", "lab1", MSKT)
  object LAB2 extends AnalysisGroup("Время свертываемости крови ВСК", "lab2", MSKT)
  object LAB3 extends AnalysisGroup("Протромбиновое время ПТИ МНО", "lab3", MSKT)
  object LAB4 extends AnalysisGroup("АЧТВ", "lab4", MSKT)
  object LAB5 extends AnalysisGroup("Протромбиновое время, соотношение", "lab5", MSKT)
  object LAB6 extends AnalysisGroup("ПТН", "lab6", MSKT)
  object LAB7 extends AnalysisGroup("Протромбиновое время", "lab7", MSKT)
  object LAB8 extends AnalysisGroup("HBsAg (Гепатит В)", "lab8", MSKT)
  object LAB9 extends AnalysisGroup("HCV (Гепатит С)", "lab9", MSKT)
  object LAB10 extends AnalysisGroup("Антистрептолизин-О (АСЛО)", "lab10", MSKT)
  object LAB11 extends AnalysisGroup("С-реактивный белок (СРБ)", "lab11", MSKT)
  object LAB12 extends AnalysisGroup("Ревматоидный фактор (РФ)", "lab12", MSKT)
  object LAB13 extends AnalysisGroup("Общий белок", "lab13", MSKT)
  object LAB14 extends AnalysisGroup("Билирубин (общий, прямой, непрямой)", "lab14", MSKT)
  object LAB15 extends AnalysisGroup("Аланиноминотрансфераза (АЛТ)", "lab15", MSKT)
  object LAB16 extends AnalysisGroup("Аланиноминотрансфераза (АЛТ)", "lab16", MSKT)
  object LAB17 extends AnalysisGroup("Креатинин", "lab17", MSKT)
  object LAB18 extends AnalysisGroup("Мочевина", "lab18", MSKT)

  val analysisTypes = Seq(MRT, MSKT, UZI, Laboratoriya)

  val analysisGroups = Seq(MRT1, MRT2, MRT3, MRT4, MRT5, MRT6, MRT7, MRT8, MRT9, MRT10, MRT11, MRT12, MRT13, MRT14, MRT15,
    MSKT1, MSKT2, MSKT3, MSKT4, MSKT5, MSKT6, MSKT7, MSKT8, MSKT9, MSKT10, MSKT11, MSKT12, MSKT13,
    UZI1 ,UZI2, UZI3, UZI4, UZI5, UZI6, UZI7, UZI8, UZI9, UZI10, UZI11, UZI12,
    LAB1, LAB2, LAB3, LAB4, LAB5, LAB6, LAB7, LAB8, LAB9, LAB10, LAB11, LAB12, LAB13, LAB14, LAB15, LAB16, LAB17, LAB18)

  def getanalysisGroupsByCode(clientCode: String): Seq[AnalysisGroup] = {
    analysisGroups.filter(_.analysisType.code == clientCode).sortBy(_.name)
  }
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
