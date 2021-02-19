package controllers

import protocols.Authentication.{LoginSessionKey, SessionAttr}

import java.security.SecureRandom
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.util.Random

trait CommonMethods {
  val Digits = 9

  def getRandomPassword(length: Int): String = {
    val algorithm = new SecureRandom
    val passwordChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray
    val password = new StringBuilder
    for (_ <- 0 to length) password.append(passwordChars(algorithm.nextInt(passwordChars.length)))
    password.toString
  }

  def createSessionAttr(role: Set[String]): SessionAttr = SessionAttr(LoginSessionKey, role)

  def getRandomDigit: Int => Int = Seq.fill(_)(Random.nextInt(Digits)).mkString("").toInt

  def getRandomDigits: Int => String = Seq.fill(_)(Random.nextInt(Digits)).mkString

  def randomStr: Int => String = new Randoms().alphanumeric.take(_).mkString

  def randomBoolean(): Boolean = {
    val boolean = Seq(true, false)
    boolean(Random.nextInt(boolean.length))
  }

  class Randoms extends Random {
    override def alphanumeric: LazyList[Char] = {
      def nextAlphaNum: Char = {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        chars charAt (Random nextInt chars.length)
      }

      LazyList continually nextAlphaNum
    }
  }

  def parseDate(dateStr: String, dateFormat: String = "dd/MM/yyyy"): LocalDate =
    LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(dateFormat))

  def parseStringToDateTime(dateStrOpt: Option[String]): Option[LocalDateTime] = {
    dateStrOpt.map { dateStr =>
      val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      LocalDateTime.parse(dateStr, formatter)
    }
  }

  def clearPhone: String => String = "[(|)|-]".r.replaceAllIn(_, "")
}