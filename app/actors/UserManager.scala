package actors

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import play.api.{Configuration, Environment}
import protocols.UserProtocol.{CheckUserByLogin, GetRoles, Roles, User, checkUserByLoginAndCreate}

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import protocols.SecurityUtils.md5

class UserManager @Inject()(val configuration: Configuration,
                            val environment: Environment)
                           (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)
  private val DoobieModule = DoobieUtil.doobieModule(configuration)

  override def receive: Receive = {
    case CheckUserByLogin(login, password) =>
      checkUserByLoginAndPassword(login, password).pipeTo(sender())

    case checkUserByLoginAndCreate(user) =>
      checkUserByLoginAndCreate(user).pipeTo(sender())

    case GetRoles =>
      getRoles.pipeTo(sender())
  }

  private def checkUserByLoginAndPassword(login: String, password: String): Future[Either[String, String]] = {
    DoobieModule.repo.getUserByLogin(login).compile.last.unsafeToFuture().map { result =>
      if (result.exists(_.password == md5(password))) {
        Right(result.get.role)
      } else {
        Left("Incorrect login or password")
      }
    }.recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while requesting Login or Password")
    }
  }

  private def checkUserByLoginAndCreate(user: User): Future[Either[String, String]] = {
    DoobieModule.repo.createUser(user.copy(password = md5(user.password))).unsafeToFuture().map { _ =>
      Right("Successfully created!")
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while create user. Error: ", error)
        if (error.getMessage.contains("duplicate")) {
          Left("Login already exists")
        } else {
          Left("Error occurred while create user")
        }
    }
  }

  private def getRoles: Future[List[Roles]] = {
    DoobieModule.repo.getRoles.unsafeToFuture()
  }

}
