import sbt._

scalaVersion := Version.scala

lazy val `scala-cheatsheet` = (project in file("."))
  .aggregate(
    akka,
    core,
    cucumber,
    doobie,
    gatling,
    kafka,
    scalacheck,
    magnolia,
    munit,
    quill,
    scalajs,
    subprojectTestInParallel1,
    subprojectTestInParallel2,
    subprojectTestInParallelForkGroup,
    sttp,
    zio,
    zioHttp,
    zioKafka
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

lazy val cucumber = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.cucumberScala  % Test,
      Library.cucumberJunit  % Test,
      Library.cucumberPico   % Test,
      Library.junitInterface % Test,
      Library.junit          % Test
    )
  )

lazy val quill = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.postgres,
      Library.h2,
      Library.quillJdbc,
      Library.quillJdbcZio,
      Library.quillJasyncPostgres,
    )
  )

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
      Library.sttpsSlf4j,
      Library.circeGeneric,
      Library.log4jApi,
      Library.log4jCore,
      Library.log4jSlf4jImpl,
    )
  )

lazy val zio = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.zio,
      Library.zioStreams,
      Library.zioJson,
      Library.zioPrelude
    )
  )

lazy val magnolia = project
  .settings(
    commonSettings,
    libraryDependencies += Library.magnolia
  )

lazy val mongo = project
  .settings(
    commonSettings,
    libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.6.0"
  )

lazy val scalajs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % Version.scalaJsDom,
      "com.lihaoyi"  %%% "utest"       % Version.uTest % Test
    ),
    jsEnv                           := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Test / fork                     := false,
    fork                            := false
  )

lazy val kafka = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.kafkaClients,
      Library.kafkaStreams,
      Library.kafkaStreamsScala,
      Library.circeCore,
      Library.circeParser,
      Library.circeGeneric,
    )
  )

lazy val zioKafka = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.zioKafka,
      Library.zioJson
    )
  )

lazy val akka = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.akka,
      Library.logback
    )
  )

lazy val zioHttp = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.zio,
      Library.zioStreams,
      Library.zioHttp,
      Library.zioHttpTest % Test
    )
  )

lazy val doobie = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.doobieCore,
      Library.doobiePostgres,
      Library.doobieHirari
    )
  )

lazy val gatling = project
  .enablePlugins(GatlingPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.gatling,
      Library.gatlingCharts,
    )
  )

lazy val commonSettings = Seq(
  version           := "1.0",
  organization      := "de.wittig",
  semanticdbEnabled := true,
  scalaVersion      := Version.scala,
  scalacOptions ++= Seq(
    "-feature",
    "-language:higherKinds",
//    "-language:strictEquality",
    "-deprecation",
    "-source:future", // für better-monadic-for, das es für Scala3 nicht mehr gibt
  ),
  Test / fork       := true, // subprojects won't run in parallel then
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF"), // Showing full stack trace
  turbo := true
)

ThisBuild / concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 2))
Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)
Global / onChangedBuildSource := ReloadOnSourceChanges
