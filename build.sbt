import Tests._
import sbt.Keys.scalaVersion
import sbt._

lazy val `scala-cheatcheet` = (project in file("."))
  .aggregate(
    core,
    magnoliaScala2,
    magnoliaScala3,
    munit,
    scalacheck,
    subprojectTestInParallel1,
    subprojectTestInParallel2,
    subprojectTestInParallelForkGroup,
    sttp,
    zio
  )

lazy val core = project
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    scalaVersion := Version.scala,
    libraryDependencies ++= Dependencies.dependencies ++ Dependencies.testDependencies,
    Defaults.itSettings,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-s", "4") // scalacheck should emit 4 examples only
  )

lazy val docs = project // new documentation project
  .in(file("cheatsheet-docs")) // important: it must not be docs/
  .settings(
    commonSettings,
    scalaVersion := Version.scala3,
    mdocVariables := Map( // Update mdocVariables to include site variables like @VERSION@.
      "VERSION" -> version.value)
  )
  .enablePlugins(MdocPlugin)

lazy val subprojectTestInParallel1 = project
  .settings(
    commonSettings,
    scalaVersion := Version.scala3,
    libraryDependencies ++= Seq(
      Library.scalatest % Test
    ),
    Test / fork := false // subprojects tests will run parallel with other subprojects
  )

lazy val subprojectTestInParallel2         = project
  .settings(
    commonSettings,
    scalaVersion := Version.scala3,
    libraryDependencies ++= Seq(
      Library.scalatest % Test
    ),
    Test / fork := false //  subprojects tests will run parallel with other subprojects
  )

ThisBuild / concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 2))
Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)
lazy val subprojectTestInParallelForkGroup = project
  .settings(
    commonSettings,
    scalaVersion := Version.scala3,
    libraryDependencies ++= Seq(
      Library.scalatest % Test
    ),
    Test / parallelExecution := true,
    Test / testForkedParallel := false, // Hier kann man auch noch innerhalb der Gruppe parallelisieren. Sollte False sein.
    Test / fork := false,
    Test / testGrouping := (Test / definedTests).value
      .groupBy(_.name.split('.')(1)(0)) // Gruppierung hier nach erstem Buchstabe der Testklasse
      .map { case (letter, tests) =>
        println(s"--------> Testgruppe $letter mit ${tests.length} Tests")
        val options = ForkOptions().withRunJVMOptions(Vector("-Dfirst.letter" + letter))
        Group(letter.toString, tests, SubProcess(options))
      }
      .toSeq
  )

lazy val munit = project
  .settings(
    commonSettings,
    scalaVersion := Version.scala3,
    libraryDependencies ++= Seq(
      Library.munit           % Test,
      Library.scalatest       % Test,
      Library.monix           % Test,
      Library.munitScalaCheck % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Test / fork := true, //  subprojects tests will run parallel with other subprojects
    Test / testOptions += Tests.Cleanup(() => println("+++++++++++++cleaned++++++++++++++++")) // Einfacher Hook
//    Test / testOptions += Tests.Cleanup(loader => loader.loadClass("munit.Cleaner").newInstance) // Laden einer Klasse // Funktioniert nur mit fork := false
  )

lazy val scalacheck = project
  .settings(
    commonSettings,
    scalaVersion := Version.scala,
    libraryDependencies += Library.scalaCheck          % Test,
    libraryDependencies += Library.shapelessScalaCheck % Test,
    Test / fork := true, //  subprojects tests will run parallel with other subprojects
  )

lazy val sttp = project
  .settings(
    commonSettings,
    scalaVersion := Version.scala3,
    scalacOptions ++= Seq("-noindent", "-rewrite"),
//    scalacOptions ++= Seq("--new-syntax", "-rewrite"),
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
    commonSettings,
    scalaVersion := Version.scala3,
    libraryDependencies ++= Dependencies.zioDependencies
  )

lazy val magnoliaScala2 = project
  .settings(
    commonSettings,
    scalaVersion := Version.scala,
    libraryDependencies ++= Seq(
      Library.magnolia,
      Library.reflect
    )
  )

lazy val magnoliaScala3 = project
  .settings(
    commonSettings,
    scalacOptions ++= Seq("-noindent", "-rewrite"),
    scalaVersion := Version.scala3,
    libraryDependencies += Library.magnolia2
  )

lazy val scalajs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    scalaVersion := Version.scala,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % Version.scalaJsDom,
      "com.lihaoyi"  %%% "utest"       % Version.uTest % Test
    ),
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    fork := false,
    parallelExecution := false
  )

lazy val commonSettings = Seq(
  version := "1.0",
  organization := "de.wittig",
  semanticdbEnabled := true,
  scalacOptions ++= Seq(
    "-language:_",
    "-encoding",
    "UTF-8",
    // Emit warning for usages of features that should be imported explicitly
    "-feature",
    // Emit warning for usages of deprecated APIs
    "-deprecation",
    // Enable additional warnings where generated code depends on assumptions
    "-unchecked",
    "-Ywarn-value-discard",
    "-Ymacro-annotations" // scala 2.13.0
  ),
  Test / fork := true, // subprojects won't run in parallel then
  // Showing full stack trace
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF"),
  //updateOptions := updateOptions.value.withCachedResolution(true),
  //updateConfiguration in updateSbtClassifiers := (updateConfiguration in updateSbtClassifiers).value.withMissingOk(true),
  turbo := true
)

Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias("ls", "projects")
addCommandAlias("cd", "project")
