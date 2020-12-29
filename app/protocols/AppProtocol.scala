package protocols

import java.time.LocalDateTime

object AppProtocol {

  case class Patient(create_at: LocalDateTime,
                     firstname: String,
                     lastname: String,
                     phone: String,
                     email: Option[String] = None,
                     passport: String,
                     customer_id: String,
                     login: String,
                     password: String) {
    def id: Option[Int] = None
  }

  case class CreatePatient(patient: Patient)
  case class GetPatientByCustomerId(customerId: String)

}
