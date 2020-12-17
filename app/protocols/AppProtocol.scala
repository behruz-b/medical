package protocols

object AppProtocol {

  case class User(id: Option[Int] = None,
                  firstName: String,
                  lastName: String,
                  passwordSerialNumber: String,
                  phoneNumber: Int,
                  email: Option[String])

}
