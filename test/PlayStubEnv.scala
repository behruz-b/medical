import java.io.File

import actors.ActorsModule
import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.{Application, Configuration, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import stub.env._

import scala.concurrent.ExecutionContextExecutor

trait PlayStubEnv extends PlaySpec with GuiceOneAppPerSuite {
  implicit val actorSystem: ActorSystem = ActorSystem("MedicalSpec")

  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  val myConfigFile = new File("medical/test/application.test.conf")

  val parsedConfig: Config = ConfigFactory.parseFile(myConfigFile)
  val configuration: Config = ConfigFactory.load(parsedConfig)

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .in(Mode.Test)
      .overrides(bind[ActorSystem].toInstance(actorSystem))
      .overrides(bind[Configuration].toInstance(Configuration(configuration)))
      .overrides(new ActorsModule() {
        override def configure(): Unit = {
          bindActor[PatientManagerStub]("patient-manager")
        }})
      .build()
  }

}
