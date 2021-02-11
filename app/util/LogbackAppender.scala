package util

import akka.actor.{ActorSelection, ActorSystem}
import akka.util.Timeout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.typesafe.scalalogging.LazyLogging
import protocols.AppProtocol.NotifyMessage

import scala.concurrent.duration.DurationInt
import scala.util.Try

trait LogbackAppender[T] extends AppenderBase[T] with LazyLogging {
  implicit val defaultTimeout: Timeout = Timeout(60.seconds)

  val actorSystem: ActorSystem
  val notifierManager: ActorSelection
  val loggingPrefix: String

  override def start(): Unit = {
    super.start()
  }

  override def stop(): Unit = {
    actorSystem.terminate()
    super.stop()
  }

  override def append(eventObject: T): Unit = {
    val msg = Try {
      eventObject match {
        case loggingEvent: ILoggingEvent =>
          val msg = loggingEvent.getFormattedMessage
          val className = loggingEvent.getLoggerName.split('.').last
          val throwableProxy = loggingEvent.getThrowableProxy
          if (throwableProxy != null) {
            s"$className | $msg | ${throwableProxy.getMessage}"
          } else {
            s"$className | $msg"
          }

        case _ =>
          eventObject.toString
      }
    }

    msg.map(sendNotification)
      .recover {
        case e: Throwable =>
          sendNotification(s"${eventObject.toString} | [logging-error] ${e.toString}")
      }
  }

  private def sendNotification(errorText: String): Unit = {
    logger.info(s"error: $errorText")
    logger.warn(s"error: $errorText")
    logger.debug(s"error: $errorText")
    notifierManager ! NotifyMessage(s"$loggingPrefix $errorText")
  }
}
