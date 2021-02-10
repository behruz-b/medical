package util

import akka.actor.{ActorSelection, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}

class NotifierLogbackAppender[T] extends LogbackAppender[T] {
  val config: Config = ConfigFactory.load("application.conf")
  val actorConfig: Config = config.getConfig("monitoring-actor")
  val monitoringActorSystemPath: String = config.getString("monitoring-notification-manager")

  override lazy val actorSystem: ActorSystem = ActorSystem("AppMonitoringNotificationSystem", actorConfig)
  override lazy val notifierManager: ActorSelection = actorSystem.actorSelection(monitoringActorSystemPath)

  override val loggingPrefix = "[medical]"
}
