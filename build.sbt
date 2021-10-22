import sbt._

scalaVersion := Version.scala

lazy val `scala-cheatsheet` = (project in file("."))
  .aggregate(
    core,
    scalacheck,
    magnolia,
    munit,
    scalajs,
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
    libraryDependencies ++= Dependencies.dependencies ++ Dependencies.testDependencies,
    Defaults.itSettings,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-s", "4") // scalacheck should emit 4 examples only
  )

lazy val docs = project // new documentation project
  .in(file("cheatsheet-docs")) // important: it must not be docs/
  .settings(
    commonSettings,
    mdocVariables := Map( // Update mdocVariables to include site variables like @VERSION@.
      "VERSION" -> version.value)
  )
  .enablePlugins(MdocPlugin)

lazy val subprojectTestInParallel1 = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.scalatest % Test
    ),
    Test / fork := false // subprojects tests will run parallel with other subprojects
  )

lazy val subprojectTestInParallel2 = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.scalatest % Test
    ),
    Test / fork := false //  subprojects tests will run parallel with other subprojects
  )

lazy val subprojectTestInParallelForkGroup = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.scalatest % Test
    ),
    Test / parallelExecution  := true,
    Test / testForkedParallel := false, // Hier kann man auch noch innerhalb der Gruppe parallelisieren. Sollte False sein.
    Test / fork               := false,
    Test / testGrouping       := (Test / definedTests).value
      .groupBy(_.name.split('.')(1)(0)) // Gruppierung hier nach erstem Buchstabe der Testklasse
      .map { case (letter, tests) =>
        println(s"--------> Testgruppe $letter mit ${tests.length} Tests")
        val options = ForkOptions().withRunJVMOptions(Vector("-Dfirst.letter" + letter))
        Tests.Group(letter.toString, tests, Tests.SubProcess(options))
      }
      .toSeq
  )

lazy val munit = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.munit           % Test,
      Library.scalatest       % Test,
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
    libraryDependencies += Library.scalaCheck % Test,
    Test / fork                              := true, //  subprojects tests will run parallel with other subprojects
  )

lazy val sttp = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.sttpCore,
      Library.sttpBEAsync,
      Library.sttpBEZio,
      Library.sttpCirce,
      Library.akkaStream,
      Library.circeGeneric,
      Library.log4jApi,
      Library.log4jCore,
      Library.log4jSlf4jImpl,
      "com.softwaremill.sttp.client3" %% "slf4j-backend" % "3.3.15"
    )
  )

lazy val zio = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.zio,
      Library.zioStreams,
      Library.zioPrelude
    )
  )

lazy val magnolia = project
  .settings(
    commonSettings,
//    scalacOptions ++= Seq("-noindent", "-rewrite"),
    libraryDependencies += Library.magnolia
  )

lazy val scalajs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % Version.scalaJsDom cross CrossVersion.for3Use2_13,
      "com.lihaoyi"  %%% "utest"       % Version.uTest % Test
    ),
    jsEnv                           := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / fork                     := false,
    fork                            := false
  )

lazy val commonSettings = Seq(
  version           := "1.0",
  organization      := "de.wittig",
  semanticdbEnabled := true,
  scalaVersion      := Version.scala,
  scalacOptions ++= Seq(
    "-feature",
    "-language:higherKinds",
    "-deprecation",
    "-source:future",               // für better-monadic-for, das es für Scala3 nicht mehr gibt
    "-Ykind-projector:underscores", // für KindProjector
    "-Ykind-projector"
  ),
  Test / fork       := true, // subprojects won't run in parallel then
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF"), // Showing full stack trace
  turbo := true
)

ThisBuild / concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 2))
Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)
Global / onChangedBuildSource := ReloadOnSourceChanges

addCommandAlias("ls", "projects")
addCommandAlias("cd", "project")
