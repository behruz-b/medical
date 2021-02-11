package actors

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[PatientManager]("patient-manager")
    bindActor[UserManager]("user-manager")
    bindActor[StatsManager]("stats-manager")
    bindActor[EmailSender]("email-sender")
    bindActor[MonitoringNotifier]("monitoring-notifier")

  }
}
