import mill._, scalalib._

object osLib extends SbtModule {
  def scalaVersion = "3.4.0"
  def forkArgs     = Seq("-Xmx4g")

  def ivyDeps = Agg(
    ivy"com.lihaoyi::os-lib:0.9.3"
  )
}

object macros extends SbtModule {
  def scalaVersion = "3.4.0"
  def forkArgs     = Seq("-Xmx4g")

}

object magnolia extends SbtModule {
  def scalaVersion = "3.4.0"

  def ivyDeps = Agg(
    ivy"com.softwaremill.magnolia1_3::magnolia:1.3.4"
  )
}

object tess4j extends SbtModule {
  def scalaVersion = "3.4.0"

  def ivyDeps = Agg(
    ivy"net.sourceforge.tess4j:tess4j:5.8.0"
  )
}

object scalacheck extends SbtModule {
  def scalaVersion = "3.4.0"

  object test extends SbtModuleTests {
    def ivyDeps       = Agg(
      ivy"org.scalacheck::scalacheck:1.15.4",
      ivy"org.scalatest::scalatest:3.2.18"
    )
//    def testFramework = "org.scalatest.tools.Framework"
    def testFramework = "org.scalacheck.ScalaCheckFramework"
  }
}
