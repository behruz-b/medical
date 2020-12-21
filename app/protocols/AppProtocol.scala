package protocols

object AppProtocol {

  //  TODO case class ichindagi polyala to'liq amas to'liq qilib keyin push atingla!
  case class Patient(firstName: String,
                     lastName: String,
                     passwordSN: String,
                     phone: String,
                     email: Option[String] = None,
                     customerId: Option[String] = None) {
    def id: Option[Int] = None
  }

  case class CreatePatients(patient: Patient)

}
