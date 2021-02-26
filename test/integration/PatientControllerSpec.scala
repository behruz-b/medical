package integration

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, route, status, _}
import protocols.Authentication.AppRole.AdminRole
import utils.PlayStubEnv

class PatientControllerSpec extends PlaySpec with GuiceOneAppPerSuite with PlayStubEnv{
  val patient: JsValue = Json.parse(
    """
    {
      "firstName": "Test firstName",
       "lastName": "Test lastname",
       "phone": "991234567",
       "dateOfBirth": "01/01/2001",
       "address": "Urganch tumani",
       "analysisType": "MRT",
       "analysisGroup": "String",
       "docFullName": "Jumanazar Jumanazarov",
       "docPhone": "991233232",
       "companyCode": "localhost",
       "docId": 1
    }""")

  val doc: JsValue = Json.parse(
    """
    {
       "fullName": "firstname lastname",
       "phone": "991234567"
    }
    """)

  "Create patient doc" should {
    "return OK" in {
      val sendRequest = route(app, FakeRequest(POST, controllers.routes.PatientController.addPatientsDoc().url)
        .withJsonBody(doc)
        .withSession(
          authInit(loginParams.sessionAttr.sessionKey, AdminRole, loginParams.sessionDuration) ++
          authInit("login", "admin", loginParams.sessionDuration): _*
        )
      ).get
      status(sendRequest) mustBe OK
    }
  }

  "Create patient" should {
    "return OK" in {
      val sendRequest = route(app, FakeRequest(POST, controllers.routes.PatientController.createPatient().url)
        .withJsonBody(patient)
        .withSession(
          authInit(loginParams.sessionAttr.sessionKey, AdminRole, loginParams.sessionDuration) ++
          authInit("login", "admin", loginParams.sessionDuration): _*
        )
      ).get
      status(sendRequest) mustBe OK
    }
  }

  "The GuiceOneAppPerSuite trait" must {
    "provide an Application" in {
      app.configuration.getOptional[String]("play.akka.actor-system") mustBe Some("medical-actor")
    }
  }

  "Get Patient doc page" should {
    "return OK" in {
      val request = route(app, FakeRequest(GET, controllers.routes.PatientController.patientsDocPage().url)
        .withSession(authInit(loginParams.sessionAttr.sessionKey, AdminRole, loginParams.sessionDuration) ++
        authInit("login", "admin", loginParams.sessionDuration): _*)
      ).get
      status(request) mustBe OK
    }
  }

}
