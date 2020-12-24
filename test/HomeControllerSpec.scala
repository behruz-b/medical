import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class HomeControllerSpec extends PlayStubEnv {
  val patient: JsValue = Json.parse(
    """
   {
      "firstName": "Test",
      "lastName": "Test",
      "passportSn": "Test",
      "phone": "Test",
      "email": "Test",
      "login": "Test",
      "password": "Test"
    }""")

  "Create patient" should {
    "return OK" in {
      val sendRequest = route(app, FakeRequest(POST, controllers.routes.HomeController.createUser().url)
        .withJsonBody(patient)
      ).get

      status(sendRequest) mustBe OK
    }
  }


}
