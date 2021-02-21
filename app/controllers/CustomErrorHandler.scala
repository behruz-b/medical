package controllers

import com.typesafe.scalalogging.LazyLogging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, InternalServerError, Ok}

trait CustomErrorHandler extends LazyLogging {

  def handleErrorWithStatus(errorText: String = "Xatolik yuz berdi!",
                            logText: String = "Error occurred",
                            status: Int = BAD_REQUEST): PartialFunction[Throwable, Result] = {
    case error: Throwable =>
      logger.error(logText, error)
      status match {
        case OK => Ok(errorText)
        case BAD_REQUEST => BadRequest(errorText)
        case INTERNAL_SERVER_ERROR => InternalServerError(errorText)
      }
  }
}
