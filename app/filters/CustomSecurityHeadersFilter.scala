package filters

import play.api.mvc._

class CustomSecurityHeadersFilter extends EssentialFilter {
  import scala.concurrent.ExecutionContext.Implicits.global

  def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
    next(req).map { result =>
      result.withHeaders("X-Frame-Options" -> "SAMEORIGIN")
    }
  }
}
