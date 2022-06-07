import sbt._

object Version {
  final val akka                    = "2.6.19"
  final val catsVersion             = "2.7.0"
  final val catsConsole             = "0.8.1"
  final val circeVersion            = "0.14.2"
  final val doobie                  = "1.0.0-RC2"
  final val fs2                     = "3.2.7"
  final val gatling                 = "3.7.6"
  final val kafka                   = "2.8.1"
  final val logback                 = "1.2.11"
  final val log4j                   = "2.17.2"
  final val magnolia                = "1.1.3"
  final val monocle                 = "3.1.0"
  final val scala                   = "3.1.2"
  final val scalaParallelCollection = "1.0.4"
  final val scodec                  = "2.1.0"
  final val sttp                    = "3.6.2"
  final val xstream                 = "1.4.19"
  final val xml                     = "2.1.0"
  final val zio1                    = "1.0.15"
  final val zio2                    = "2.0.0-RC6"
  final val zioJson                 = "0.3.0-RC8"
  final val zioHttp                 = "2.0.0-RC9"
  final val zioPrelude              = "1.0.0-RC10"

  // ScalaJS
  final val scalaJsDom = "2.2.0"
  final val uTest      = "0.7.11"

  // Test
  final val mockito       = "1.10.19"
  final val munit         = "0.7.29"
  final val scalaTest     = "3.2.12"
  final val selenium      = "4.2.1"
  final val seleniumPlus  = "3.2.10.0"
  final val scalaCheck    = "1.16.0"
  final val testContainer = "0.40.8"
}

object Library {
  final val akka                    = "com.typesafe.akka"             %% "akka-actor-typed"                 % Version.akka
  final val catsCore                = "org.typelevel"                 %% "cats-core"                        % Version.catsVersion
  final val catsFree                = "org.typelevel"                 %% "cats-free"                        % Version.catsVersion
  final val circeCore               = "io.circe"                      %% "circe-core"                       % Version.circeVersion
  final val circeGeneric            = "io.circe"                      %% "circe-generic"                    % Version.circeVersion
  final val circeParser             = "io.circe"                      %% "circe-parser"                     % Version.circeVersion
  final val fs2Core                 = "co.fs2"                        %% "fs2-core"                         % Version.fs2
  final val doobieCore              = "org.tpolecat"                  %% "doobie-core"                      % Version.doobie
  final val doobiePostgres          = "org.tpolecat"                  %% "doobie-postgres"                  % Version.doobie
  final val doobieHirari            = "org.tpolecat"                  %% "doobie-hikari"                    % Version.doobie
  final val fs2IO                   = "co.fs2"                        %% "fs2-io"                           % Version.fs2
  final val fs2ReactiveStreams      = "co.fs2"                        %% "fs2-reactive-streams"             % Version.fs2
  final val kafkaClients            = "org.apache.kafka"               % "kafka-clients"                    % Version.kafka
  final val kafkaStreams            = "org.apache.kafka"               % "kafka-streams"                    % Version.kafka
  final val kafkaStreamsScala       = "org.apache.kafka"              %% "kafka-streams-scala"              % Version.kafka cross CrossVersion.for3Use2_13
  final val logback                 = "ch.qos.logback"                 % "logback-classic"                  % Version.logback
  final val log4jApi                = "org.apache.logging.log4j"       % "log4j-api"                        % Version.log4j
  final val log4jCore               = "org.apache.logging.log4j"       % "log4j-core"                       % Version.log4j
  final val log4jSlf4jImpl          = "org.apache.logging.log4j"       % "log4j-slf4j-impl"                 % Version.log4j
  final val magnolia                = "com.softwaremill.magnolia1_3"  %% "magnolia"                         % Version.magnolia
  final val monocle                 = "dev.optics"                    %% "monocle-core"                     % Version.monocle
  final val scalaParallelCollection = "org.scala-lang.modules"        %% "scala-parallel-collections"       % Version.scalaParallelCollection
  final val scodec                  = "org.scodec"                    %% "scodec-core"                      % Version.scodec
  final val sttpCore                = "com.softwaremill.sttp.client3" %% "core"                             % Version.sttp
  final val sttpCirce               = "com.softwaremill.sttp.client3" %% "circe"                            % Version.sttp
  final val sttpBEAsync             = "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % Version.sttp
  final val sttpBEZio               = "com.softwaremill.sttp.client3" %% "zio"                              % Version.sttp
  final val sttpsSlf4j              = "com.softwaremill.sttp.client3" %% "slf4j-backend"                    % Version.sttp
  final val xstream                 = "com.thoughtworks.xstream"       % "xstream"                          % Version.xstream
  final val xml                     = "org.scala-lang.modules"        %% "scala-xml"                        % Version.xml
  final val zio1                    = "dev.zio"                       %% "zio"                              % Version.zio1
  final val zioStreams1             = "dev.zio"                       %% "zio-streams"                      % Version.zio1
  final val zio2                    = "dev.zio"                       %% "zio"                              % Version.zio2
  final val zioStreams2             = "dev.zio"                       %% "zio-streams"                      % Version.zio2
  final val zio2Json                = "dev.zio"                       %% "zio-json"                         % Version.zioJson
  final val zioPrelude1             = "dev.zio"                       %% "zio-prelude"                      % Version.zioPrelude
  final val zioHttp                 = "io.d11"                        %% "zhttp"                            % Version.zioHttp

  // test
  final val gatlingCharts   = "io.gatling.highcharts"   % "gatling-charts-highcharts"      % Version.gatling
  final val gatling         = "io.gatling"              % "gatling-test-framework"         % Version.gatling
  final val mockito         = "org.mockito"             % "mockito-all"                    % Version.mockito
  final val munit           = "org.scalameta"          %% "munit"                          % Version.munit
  final val munitScalaCheck = "org.scalameta"          %% "munit-scalacheck"               % Version.munit
  final val scalatest       = "org.scalatest"          %% "scalatest"                      % Version.scalaTest
  final val selenium        = "org.seleniumhq.selenium" % "selenium-java"                  % Version.selenium
  final val seleniumPlus    = "org.scalatestplus"      %% "selenium-3-141"                 % Version.seleniumPlus
  final val scalaCheck      = "org.scalacheck"         %% "scalacheck"                     % Version.scalaCheck
  final val testContainer   = "com.dimafeng"           %% "testcontainers-scala-scalatest" % Version.testContainer
  final val zioHttpTest     = "io.d11"                 %% "zhttp-test"                     % Version.zioHttp
}

object Dependencies {
  import Library._
  val dependencies = Seq(
    catsCore,
    catsFree,
    circeCore,
    circeGeneric,
    circeParser,
    fs2Core,
    fs2IO,
    fs2ReactiveStreams,
    logback,
    monocle,
    scalaParallelCollection,
    scodec,
    xstream,
    xml,
    zio1
  )

  val testDependencies = Seq(
    mockito       % Test,
    scalatest     % "it,test",
    seleniumPlus  % "it",
    selenium      % "it",
    scalaCheck    % Test,
    testContainer % Test,
  )
}
