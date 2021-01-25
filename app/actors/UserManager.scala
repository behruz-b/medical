package actors

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import doobie.common.DoobieUtil
import javax.inject.Inject
import play.api.{Configuration, Environment}
import protocols.UserProtocol.{CheckUserByLogin, CreateUser, checkUserByLoginAndCreate, User}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class UserManager @Inject()(val configuration: Configuration,
                            val environment: Environment)
                           (implicit val ec: ExecutionContext)
  extends Actor with LazyLogging {

  implicit val defaultTimeout: Timeout = Timeout(60.seconds)
  private val DoobieModule = DoobieUtil.doobieModule(configuration)

  override def receive: Receive = {
    case CheckUserByLogin(login, password) =>
      checkUserByLoginAndPassword(login, password).pipeTo(sender())

//    case CreateUser(user) =>
//      createUser(user).pipeTo(sender())

    case checkUserByLoginAndCreate(user) =>
      checkUserByLoginAndCreate(user).pipeTo(sender())
  }

//  private def createUser(user: User): Future[Either[String, String]] = {
//    (for {
//      result <- DoobieModule.repo.createUser(user).unsafeToFuture()
//    } yield {
//      logger.debug(s"result: $result")
//      Right("Successfully User created")
//    }).recover {
//      case e: Throwable =>
//        logger.error("Error", e)
//        Left("Error happened while creating user")
//    }
//  }

  private def checkUserByLoginAndPassword(login: String, password: String): Future[Either[String, String]] = {
    logger.debug(s"login: $login, password: $password")
    (for {
      result <- DoobieModule.repo.getUserByLogin(login).compile.last.unsafeToFuture()
    } yield {
      logger.debug(s"result: ${result.exists(_.password == password)}, $result")
      if (result.exists(_.password == password)) {
        Right(result.get.role)
      } else {
        Left("Incorrect login or password")
      }
    }).recover {
      case e: Throwable =>
        logger.error("Error", e)
        Left("Error happened while requesting Login or Password")
    }
  }

  private def checkUserByLoginAndCreate(user: User): Future[Either[String, String]] = {
    DoobieModule.repo.createUser(user).unsafeToFuture().map { _ =>
      Right("Successfully created!")
    }.recover {
      case error: Throwable =>
        logger.error("Error occurred while create user. Error: ", error)
        if (error.getMessage.contains("duplicate")){
          Left("Login already exists")
        } else {
          Left("Error occurred while create user")
        }
    }
  }

}
