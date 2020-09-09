import sbt.Test

ThisBuild / version := "1.0"
ThisBuild / scalaVersion := Version.scala
ThisBuild / organization := "de.wittig"
ThisBuild / scalacOptions ++= Seq(
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
  "-Ymacro-annotations"         // scala 2.13.0
)
// Change this to another test framework if you prefer
ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")
ThisBuild / resolvers += Resolver.sonatypeRepo("public")
ThisBuild / resolvers += "bintray/non" at "https://dl.bintray.com/non/maven"
ThisBuild / resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  Resolver.bintrayRepo("hseeberger", "maven")
)
ThisBuild / Test / fork := true // subprojects won't run in parallel then
//ThisBuild / updateOptions := updateOptions.value.withCachedResolution(true)
//ThisBuild / updateConfiguration in updateSbtClassifiers := (updateConfiguration in updateSbtClassifiers).value.withMissingOk(true)

ThisBuild / turbo := true
ThisBuild / onChangedBuildSource := ReloadOnSourceChanges

// Showing full stack trace
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF")

lazy val `scala-cheatcheet` = (project in file("."))
  .aggregate(
    core,
    munit,
    subprojectTestInParallel1,
    subprojectTestInParallel2,
    sttp,
    zio
  )

lazy val core = project
  .configs(IntegrationTest)
  .settings(
    libraryDependencies ++= Dependencies.dependencies ++ Dependencies.testDependencies,
    Defaults.itSettings,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-s", "4") // scalacheck should emit 4 examples only
  )

lazy val docs = project // new documentation project
  .in(file("cheatsheet-docs")) // important: it must not be docs/
  .settings(
    mdocVariables := Map( // Update mdocVariables to include site variables like @VERSION@.
      "VERSION" -> version.value
    )
  )
  .enablePlugins(MdocPlugin)

lazy val subprojectTestInParallel1 = project
  .settings(
    libraryDependencies ++= Seq(
      Library.scalatest % Test
    ),
    Test / fork := false // subprojects tests will run parallel with other subprojects
  )
lazy val subprojectTestInParallel2 = project
  .settings(
    libraryDependencies ++= Seq(
      Library.scalatest % Test
    ),
    Test / fork := false //  subprojects tests will run parallel with other subprojects
  )

lazy val munit = project
  .settings(
    libraryDependencies += Library.munit % Test,
    testFrameworks += new TestFramework("munit.Framework"),
    Test / fork := false //  subprojects tests will run parallel with other subprojects
  )

lazy val sttp = project
  .settings(
    libraryDependencies ++= Seq(
      Library.sttpCore,
      Library.sttpBEAsync,
      Library.sttpBEAkkaHttp,
      Library.sttpBEMonix,
      Library.sttpBEZio,
      Library.sttpCirce,
      Library.akkaStream,
      Library.circeGeneric
    )
  )

lazy val zio = project
  .settings(
    libraryDependencies ++= Dependencies.dependencies ++ Dependencies.zioDependencies
  )
