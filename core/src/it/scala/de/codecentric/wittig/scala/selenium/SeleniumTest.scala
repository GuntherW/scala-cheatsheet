package de.codecentric.wittig.scala.selenium

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.selenium.HtmlUnit

class SeleniumTest extends AnyFunSuite with Matchers with HtmlUnit {

  test("The blog app home page should have the correct title") {
    go to "https://kde.org"
    assert(pageTitle.contains("KDE"))
  }
}
