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
//    "-source:future",
    "-Ybackend-parallelism:8",
    "-Xlint",
    "-experimental",
    "-unchecked",
    "-release:23",
    "-Wunused:imports", // for scalafix
//    "-language:strictEquality"
    // "-Vprofile"
  ),
  Test / fork       := true, // subprojects won't run in parallel then
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oF"), // Showing full stack trace
  turbo             := true,
  usePipelining     := true,
  scalafixOnCompile := true,
)

ThisBuild / concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 2))
Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)
Global / onChangedBuildSource      := ReloadOnSourceChanges

lazy val `scala-cheatsheet` = (project in file("."))
  .aggregate(
    akka,
    caliban,
    core,
    config,
    cdk,
    cucumber,
    database,
    datatransformation,
    http4s,
    gatling,
    grpcFs2,
    json,
    kafka,
    direct,
    macros,
    macwire,
    magnolia,
    mongo,
    munit,
    osLib,
    quill,
    scalacheck,
    scalajs,
    sttp,
    tapir,
    zio,
    ziocli,
    zioHttp,
    zioKafka,
    zioSchema,
    openAI,
  )

lazy val caliban = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.calibanQuick,
      Library.calibanClient,
      Library.osLib,
      Library.sttpCore
    ),
  ).enablePlugins(CalibanPlugin)

lazy val core = project
  .settings(
    commonSettings,
    libraryDependencies ++= Dependencies.dependencies ++ Dependencies.testDependencies,
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-s", "4") // scalacheck should emit 4 examples only
  )

lazy val cdk = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.awsCdk,
      "software.constructs" % "constructs" % "10.4.2"
    )
  )

lazy val config = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.pureConfig,
      Library.ciris,
    )
  )

lazy val json = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.circeCore,
      Library.circeGeneric,
      Library.circeParser,
      Library.jsoniter,
      Library.jsoniterMacros,
      Library.upickle,
      Library.zioJson,
    )
  )

lazy val grpcFs2 = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "io.grpc"          % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
      Library.http4sEmberServer,
      Library.http4sDsl,
      Library.http4sCirce,
      Library.weaverCats % Test
    )
  ).enablePlugins(Fs2Grpc)

lazy val docs = project // new documentation project
  .in(file("mdocs")) // important: it must not be docs/
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

lazy val datatransformation = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.ducktape,
      Library.chimney,
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
      Library.logback
    )
  )

lazy val direct = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.gears,
      Library.kyoCore,
      Library.kyoDirect,
      Library.kyoCache,
      Library.kyoStat,
      Library.kyoSttp,
      Library.kyoTapir,
      Library.ox,
      Library.tapirNettyServerSync,
      Library.sttpCore
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
    scalacOptions ++= Seq(
      "-Xcheck-macros",
      "-Ycheck:all",   // also for checking macros,
      "-Ycheck-mods",
      "-Ydebug-type-error",
      "-Xprint-types", // Without this flag, we will not see error messages for exceptions during given-macro expansion!
      "-Yshow-print-errors",
      "-language:experimental.macros",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:dynamics",
      "-unchecked",
    ),
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
      Library.logback,
      "io.github.greenleafoss"  %% "green-leaf-mongo-circe" % "3.1",
      Library.testContainerMongo % Test,
      Library.testContainer      % Test,
      Library.scalatest          % Test,
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
    libraryDependencies += Library.zioSchema,
    libraryDependencies += Library.zioSchemaJson,
    libraryDependencies += Library.zioSchemaBson,
    libraryDependencies += Library.zioSchemaProtobuf,
    libraryDependencies += Library.zioSchemaDerivation,
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
      Library.pulsar4s,
      Library.pulsar4sCirce,
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

lazy val database = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.doobieCore,
      Library.doobiePostgres,
      Library.doobieHirari,
      Library.magnum,
      Library.magnumpg,
      Library.pureConfig,
      Library.skunk,
      Library.tyqu,
      Library.logback
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

lazy val macwire = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.macwire,
    )
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

lazy val tapir = project
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      Library.http4sBlazeServer,
      Library.tapirAwsLambda,
      Library.tapirAwsCdk,
      Library.tapirAwsSam,
      Library.tapirHttp4sServer,
      Library.tapirJsonCirce,
      Library.tapirJdkHttp,
      Library.tapirNettyFuture,
      Library.ox,
      Library.tapirNettyServerSync,
      Library.tapirSwaggerUiBundle,
      Library.tapirPrometheusMetrics,
      Library.tapirSttpStubServer % Test,
      Library.sttpCirce           % Test,
      Library.scalatest           % Test,
    )
  )
