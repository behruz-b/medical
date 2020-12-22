package controllers

import scala.util.Random

trait CommonMethods {
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