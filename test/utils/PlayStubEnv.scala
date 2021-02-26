package utils

import actors.ActorsModule
import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import controllers.routes
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration, Mode}
import protocols.Authentication.AppRole.AdminRole
import protocols.Authentication.{Login, createSessionAttr}
import stub.env._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait PlayStubEnv extends PlaySpec with GuiceOneAppPerSuite {
  implicit val actorSystem: ActorSystem = ActorSystem("medical-actor")

  implicit val executionContext: ExecutionContext = actorSystem.dispatcher
  val loginParams: Login = Login("/admin/", routes.HomeController.admin(), createSessionAttr(Set(AdminRole)))
  val configuration: Config = ConfigFactory.load("resources/application.test.conf")

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .in(Mode.Test)
      .overrides(bind[ActorSystem].toInstance(actorSystem))
      .overrides(bind[Configuration].toInstance(Configuration(configuration)))
      .overrides(new ActorsModule() {
        override def configure(): Unit = {
          bindActor[PatientManagerStub]("patient-manager")
          bindActor[UserManagerStub]("user-manager")
          bindActor[StatsManagerStub]("stats-manager")
          bindActor[PatientsDocManagerStub]("patients-doc-manager")
          bindActor[EmailSenderStub]("email-sender")
          bindActor[MonitoringNotifierStub]("monitoring-notifier")
        }
      })
      .build()
  }

  def expiresAtSessionAttrName: String => String = _ + ".exp"

  def authInit(sessionAttrName: String,
               sessionAttrVal: String,
               sessionDuration: Option[FiniteDuration] = None): Seq[(String, String)] = {
    val expiresAtSessionAttr = expiresAtSessionAttrName(sessionAttrName)
    sessionDuration.foldLeft(Map(sessionAttrName -> sessionAttrVal)) { (acc, sessionDur) =>
      val nextExpiration = System.currentTimeMillis() + sessionDur.toMillis
      acc + (expiresAtSessionAttr -> nextExpiration.toString)
    }.toSeq
  }

}
