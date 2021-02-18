package protocols

import com.typesafe.config.Config
import play.api.Configuration
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, __}
import play.api.libs.mailer.{Email, SMTPConfiguration}

object AppProtocol {
  val DefaultPageSize = 30

  def smtpConfig(configuration: Configuration, path: String): SMTPConfiguration = {
    SMTPConfiguration(configuration.get[Configuration](path).underlying)
  }

  def smtpConfig(config: Config, path: String): SMTPConfiguration = {
    SMTPConfiguration(config.getConfig(path))
  }

  case class SendMail(email: Email, smtpConfig: SMTPConfiguration, recipients: Seq[String])
  case class SendSingleMail(email: Email, smtpConfig: SMTPConfiguration)

  case class NotifyMessage(message: String)
  object Paging {
    case class PageReq(page: Int = 1,
                       size: Int = DefaultPageSize,
                       isPagination: Boolean = true) {
      def offset: Int = (page - 1) * size

      def toPageRes[T](items: List[T]): PageRes[T] = {
        val pageItems =
          if (isPagination) {
            items.slice(offset, offset + size)
          } else {
            items
          }
        PageRes(items = pageItems, total = items.size)
      }
    }

    case class PageRes[T](items: List[T], total: Int)

    implicit def pageFormat[T: Format]: Format[PageRes[T]] =
      ((__ \ "items").format[List[T]] ~
        (__ \ "total").format[Int])(PageRes.apply, unapply(PageRes.unapply))
  }

}
