package de.codecentric.wittig.scala.monocle

import monocle.Lens
import monocle.macros.{GenLens, Lenses}

import scala.language.higherKinds

/**
  * Bester Weg: Mit Annotations. Am wenigsten Boilerplate, aber: Man muß Zugriff auf die Caseklassen haben.
  * Zweitbester Weg: Ins Companionobjekt schieben.
  */
object Main extends App {

  @Lenses case class Street(name: String, number: Int) // ... means it contains other fields
  @Lenses("_") case class Address(street: Street)
  @Lenses case class Company(address: Address)
  @Lenses case class Employee(company: Company)

  object Street {
    //    val name: Lens[Street, String] = GenLens[Street](_.name)
  }
  object Address {
    val street: Lens[Address, Street] = GenLens[Address](_.street)
  }
  object Company {
    //  val address: Lens[Company, Address] = GenLens[Company](_.address)
  }
  object Employee {
    //  val company: Lens[Employee, Company] = GenLens[Employee](_.company)
    val capizalizeStreet: (Employee => Employee) =
      (Employee.company composeLens Company.address composeLens Address.street composeLens Street.name)
        .modify(_.capitalize)
    def capizalizeStreet2(e: Employee): Employee =
      (Employee.company composeLens Company.address composeLens Address._street composeLens Street.name)
        .modify(_.capitalize)(e)
  }

  val employee: Employee = Employee(Company(Address(Street("hallo", 33))))

  val neu =
    (Employee.company composeLens Company.address composeLens Address._street composeLens Street.name)
      .modify(_.capitalize)(employee)
  val neu2 =
    (Employee.company composeLens Company.address composeLens Address._street composeLens Street.number)
      .modify(_ + 1)(employee)

  println(neu)
  println(neu2)
  println(Employee.capizalizeStreet(employee))
  val s = Street("kkk", 22)
  val sn = Street.number.set(44)(s)
  println(sn)

  // Oder ohne Annotation
  println(GenLens[Employee](_.company.address.street.number).set(11)(employee))
}
