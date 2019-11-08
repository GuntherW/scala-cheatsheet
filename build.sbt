name := """scala-cheatsheet"""

version := "1.0"

scalaVersion := Version.scala

organization := "de.wittig"
lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings
  )
scalacOptions ++= Seq(
  "-language:_",
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  // Emit warning for usages of features that should be impoirted explicitly
  "-feature",
  // Emit warning for usages of deprecated APIs
  "-deprecation",
  // Enable additional warnings where generated code depends on assumptions
  "-unchecked",
  // Fail the compilation if there are any warnings
  //"-Xfatal-warnings",
  // Enable or disable specific warnings
  "-Xlint:_",
  //Do not adapt an argument list to match the receiver -> z.B. List(1,2,3).toSet()
  "-Ywarn-dead-code",
  // Warn when local and private vals, vars, defs, and types are unused
//  "-Ywarn-unused",
  // Warn when imports are unused
  // "-Ywarn-unused-import",
  // Warn when non-Unit expression results are unused
  "-Ywarn-value-discard",
  "-Ymacro-annotations" // scala 2.13.0
)

val akkaVersion    = "2.5.26"
val catsVersion    = "2.0.0"
val circeVersion   = "0.12.3"
val fs2            = "2.1.0"
val kittensVersion = "2.0.0"
val monixVersion   = "3.0.0"
val monocleVersion = "2.0.0"
val scala          = "2.13.1"
val zioVersion     = "1.0.0-RC16"

// Change this to another test framework if you prefer
libraryDependencies ++= Dependencies.dependencies ++ Dependencies.testDependencies

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("public")
resolvers += "bintray/non" at "https://dl.bintray.com/non/maven"

updateOptions := updateOptions.value.withCachedResolution(true)

updateConfiguration in updateSbtClassifiers := (updateConfiguration in updateSbtClassifiers).value.withMissingOk(true)

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  Resolver.bintrayRepo("hseeberger", "maven")
)

Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-s", "4") // scalacheck should emit 4 examples only

turbo := true
Global / onChangedBuildSource := ReloadOnSourceChanges

scalafmtOnCompile := true
