package filters

import javax.inject.Inject
import play.api.http.HttpFilters

class CustomFilters @Inject()(securityHeadersFilter: CustomSecurityHeadersFilter
                             )
  extends HttpFilters {

  val filters = Seq(securityHeadersFilter)
}
