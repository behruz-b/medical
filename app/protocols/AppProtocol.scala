package protocols

object AppProtocol {

  case class User(firstName: String,
                  lastName: String,
                  passwordSN: String,
                  phone: String,
                  email: Option[String] = None,
                  customerId: Option[String] = None) {
    def id: Option[Int] = None
  }

}
