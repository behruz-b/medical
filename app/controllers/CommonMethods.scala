package controllers

import java.security.SecureRandom

import scala.util.Random

trait CommonMethods {
  def getRandomPassword(length: Int): String = {
    val algorithm = new SecureRandom
    val passwordChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray
    val password = new StringBuilder
    for (_ <- 0 to length) password.append(passwordChars(algorithm.nextInt(passwordChars.length)))
    password.toString
  }

  def getRandomDigit(length: Int): Int = {
    Seq.fill(length)(Random.nextInt(9)).mkString("").toInt
  }

  def getRandomDigits(length: Int): String = {
    Seq.fill(length)(Random.nextInt(9)).mkString
  }

  def randomStr(length: Int): String = {
    new Randoms().alphanumeric.take(length).mkString
  }

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


}