import sbt.*

scalaVersion := Version.scala

lazy val commonSettings = Seq(
  version           := "1.0",
  organization      := "de.wittig",
  semanticdbEnabled := true,
  scalaVersion      := Version.scala,
  scalacOptions ++= Seq(
    "-feature",
    "-language:higherKinds",
    "-deprecation",
    "-source:future-migration",
    // "-Vprofile"
  ),
  Test / fork       := true, // subprojects won't run in parallel then
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF"), // Showing full stack trace
  turbo := true
)

ThisBuild / concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 2))
Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)
Global / onChangedBuildSource      := ReloadOnSourceChanges

lazy val `scala-cheatsheet` = (project in file("."))
  .aggregate(
    akka,
    core,
    cucumber,
    doobie,
    http4s,
    gatling,
    kafka,
    scalacheck,
    storch,
    macros,
    magnolia,
    mongo,
    munit,
    osLib,
    quill,
    scalajs,
    sttp,
    tyqu,
    zio,
    ziocli,
    zioHttp,
    zioKafka,
    zioSchema,
    openAI,
  )

lazy val core = project
  .settings(
    commonSettings,
    libraryDependencies ++= Dependencies.dependencies ++ Dependencies.testDependencies,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-s", "4") // scalacheck should emit 4 examples only
  )

lazy val storch = project
  .settings(
    commonSettings,
    resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
    libraryDependencies ++= Seq(
      "dev.storch"  %% "core"     % "0.0-b634ff1-SNAPSHOT",
      "org.bytedeco" % "pytorch"  % "2.1.0-1.5.10-SNAPSHOT",
      "org.bytedeco" % "pytorch"  % "2.1.0-1.5.10-SNAPSHOT" classifier "linux-x86_64-gpu",
      "org.bytedeco" % "openblas" % "0.3.24-1.5.10-SNAPSHOT" classifier "linux-x86_64",
      "org.bytedeco" % "cuda"     % "12.3-8.9-1.5.10-SNAPSHOT",
      "org.bytedeco" % "cuda"     % "12.3-8.9-1.5.10-SNAPSHOT" classifier "linux-x86_64",
      "org.bytedeco" % "cuda"     % "12.3-8.9-1.5.10-SNAPSHOT" classifier "linux-x86_64-redist"
    )
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

lazy val osLib = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.osLib
    )
  )

lazy val macros = project
  .settings(commonSettings)

lazy val munit = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.munit           % Test,
      Library.scalatest       % Test,
      Library.munitScalaCheck % Test,
      Library.circeCore       % Test,
      Library.circeGeneric    % Test,
      Library.circeParser     % Test,
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
      Library.sttpOAuth,
      Library.catsCore,
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
      Library.zioPrelude,
      Library.zioTest      % Test,
      Library.zioTestSbt   % Test,
      Library.zioTestJUnit % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val magnolia = project
  .settings(
    commonSettings,
    libraryDependencies += Library.magnolia
  )

lazy val mongo = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.mongoDriverJava,
    )
  )

lazy val ziocli = project
  .settings(
    commonSettings,
    libraryDependencies += Library.zioCli,
  )

lazy val zioSchema = project
  .settings(
    commonSettings,
    libraryDependencies += Library.zio,
    libraryDependencies += Library.zioSchema,
    libraryDependencies += Library.zioSchemaJson,
    libraryDependencies += Library.zioSchemaProtobuf,
    libraryDependencies += Library.zioSchemaDerivation,
//    libraryDependencies += Library.scalaReflect,
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
      Library.zioJson
    ),
  )

lazy val openAI = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.zioOpenAI,
      Library.sttpOpenAi,
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

lazy val metaprogramming = project
  .settings(
    commonSettings
  )

lazy val http4s = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.ciris,
      Library.cirisCirce,
      Library.circeCore,
      Library.circeParser,
      Library.http4s,
      Library.http4sEmberServer,
      Library.http4sEmberClient,
      Library.http4sDsl,
      Library.jwtHttp4s,
      Library.jwtScala,
      Library.jwtCirce,
      Library.pureConfig
    )
  )

lazy val tyqu = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.tyqu,
      Library.postgres
    )
  )
