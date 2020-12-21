package protocols

import java.util.Date


object AppProtocol {

  //  TODO case class ichindagi polyala to'liq amas to'qil qilib keyin push etingla!
  case class Patient(createAt: Date,
                     firstName: String,
                     lastName: String,
                     phone: String,
                     email: Option[String] = None,
                     passportSn: String,
                     login: String,
                     password: String,
                     customerId: Option[String] = None) {
    def id: Option[Int] = None
  }

  case class CreatePatients(patient: Patient)

}
