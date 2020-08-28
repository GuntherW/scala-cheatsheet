package de.codecentric.wittig.scala.selenium.firefox

import org.openqa.selenium.firefox.{FirefoxBinary, FirefoxDriver, FirefoxOptions, FirefoxProfile}

/**
  * Convenience object that builds a new FirefoxDriver on request
  */
object FirefoxWebDriverBuilder {

  /**
    * Configures a new FirefoxDriver that is headless, is linked to the geckodriver in resources
    * and uses a direct connection (no proxy)
    * @return Pre-configured FirefoxDriver
    */
  def build(): FirefoxDriver = {

    // Configure FF to run headless
    val firefoxBinary = new FirefoxBinary()
    firefoxBinary.addCommandLineOptions("--headless")

    // Tell Selenium to use Geckodriver
    val geckoDriverResource = getClass.getClassLoader.getResource("geckodriver")
    val geckoDriverPath     = geckoDriverResource.getPath
    System.setProperty("webdriver.gecko.driver", geckoDriverPath)

    // Disable any proxy since we only access local resources. Firefox uses the system-proxy by default.
    val firefoxProfile = new FirefoxProfile()

    val firefoxOptions = new FirefoxOptions()
    firefoxOptions.setBinary(firefoxBinary)
    firefoxOptions.setProfile(firefoxProfile)

    new FirefoxDriver(firefoxOptions)
  }

}
