package protocols

import java.security.MessageDigest
import java.util.Base64

import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}


object SecurityUtils {
  private val EncrAlgorithm = "AES/CBC/PKCS5Padding"
  private val IvSpec = new IvParameterSpec(new Array[Byte](16))

  def encrypt(key: String, text: String): String = {
    val cipher = Cipher.getInstance(EncrAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, EncrSpecKey(key), IvSpec)
    new String(Base64.getEncoder.encode(cipher.doFinal(text.getBytes("utf-8"))), "utf-8")
  }

  def decrypt(key: String, encrText: String): String = {
    val cipher = Cipher.getInstance(EncrAlgorithm)
    cipher.init(Cipher.DECRYPT_MODE, EncrSpecKey(key), IvSpec)
    new String(cipher.doFinal(Base64.getDecoder.decode(encrText.getBytes("utf-8"))), "utf-8")
  }

  private def EncrSpecKey(key: String) = new SecretKeySpec(Base64.getDecoder.decode(key), "AES")

  def md5(s: String): String = {
    MessageDigest.getInstance("MD5").digest(s.getBytes).map("%02x".format(_)).mkString
  }
}