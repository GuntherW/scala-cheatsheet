package de.codecentric.wittig.scala.test.selenium

import org.scalatest.FunSuite
import org.scalatest.selenium.HtmlUnit
import org.openqa.selenium.WebDriver

/**
 * @author gunther
 */
class HtmlUnitSeleniumTest extends FunSuite with HtmlUnit {

  test("Google Titel") {
    go to "http://www.google.de"
    assert(pageTitle == "Google")
  }

  test("Google Suche") {

    webDriver.setJavascriptEnabled(false)
    go to "http://www.google.de"

    click on "q"
    //    click on id("q") // to lookup by id "q"
    //    click on name("q") // to lookup by name "q"
    textField("q").value = "Käse"

    submit()

    // Google's search is rendered dynamically with JavaScript.
    assert(pageTitle == "Käse - Google-Suche")
  }

  test("Pro-Linux  ++ Klick auf Link") {

    go to "http://www.pro-linux.de"

    click on linkText("Artikel")

    assert(pageTitle == "Artikel - Pro-Linux")
  }

}
