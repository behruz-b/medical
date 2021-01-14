package util

object StringUtil {

  def maskMiddlePart(str: String, charsLeft: Int = 2, charsRight: Int = 2, maskChar: String = "X"): String = {
    if (str.length > charsLeft + charsRight) {
      str.take(charsLeft) + maskChar * (str.length - charsLeft - charsRight) + str.takeRight(charsRight)
    } else {
      maskChar * str.length
    }
  }

}
