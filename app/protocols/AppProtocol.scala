package protocols

object AppProtocol {

  case class User(firstName: String,
                  lastName: String,
                  passwordSN: String,
                  phone: Int,
                  email: Option[String] = None,
                  customerId: Option[Int] = None) {
    def id: Option[Int] = None
  }

}
