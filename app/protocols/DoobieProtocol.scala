package protocols

import play.api.libs.json.Json

object DoobieProtocol {

  case class DoobieTest(name: String, login: String)
//  implicit val doobieTestFormat = Json.format[DoobieTest]

}
