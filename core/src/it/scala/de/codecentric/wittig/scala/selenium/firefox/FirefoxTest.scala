package de.codecentric.wittig.scala.selenium.firefox

import org.openqa.selenium.WebDriver
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.selenium.{Firefox, HtmlUnit, WebBrowser}

class FirefoxTest extends AnyFunSuite with Matchers with WebBrowser {

  implicit val webDriver: WebDriver = FirefoxWebDriverBuilder.build()

  test("The blog app home page should have the correct title") {
    go to "https://kde.org"
    assert(pageTitle.contains("KDE"))
  }
}

class FirefoxTest2 extends AnyFunSuite with Matchers with Firefox {

  test("The blog app home page should have the correct title") {
    go to "https://kde.org"
    assert(pageTitle.contains("KDE"))
  }
}
