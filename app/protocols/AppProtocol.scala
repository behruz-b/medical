package protocols

import java.time.LocalDateTime

object AppProtocol {

  case class Patient(createAt: LocalDateTime,
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
