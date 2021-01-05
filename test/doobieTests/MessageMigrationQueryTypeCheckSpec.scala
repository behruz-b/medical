package doobieTests

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import doobie.repository.MessageSQL._
import doobieTests.DbConfig.DbTransactor
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import protocols.AppProtocol.Patient

import java.time.LocalDateTime

class MessageMigrationQueryTypeCheckSpec extends funsuite.AnyFunSuite with Matchers with doobie.scalatest.IOChecker {

  override val transactor = DbTransactor
  val samplePatient: Patient = Patient(
    created_at = LocalDateTime.now(),
    firstname = "Nick",
    lastname = "Fury",
    phone = "11234567891",
    email = "nicck.fury@mail.com".some,
    passport = "AA1112233",
    customer_id = "G-324",
    login = "nickf",
    password = "diek3Jdua"
  )

  test("Create Chats in DB") {IO(create(samplePatient))}
  test("Get Chats") {IO(getByCustomerId("G-"))}

}
