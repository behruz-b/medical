package protocols

object AppProtocol {

  //  TODO case class ichindagi polyala to'liq amas to'qil qilib keyin push etingla!
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
