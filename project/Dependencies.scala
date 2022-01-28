import sbt._

object Version {
  final val catsVersion             = "2.7.0"
  final val catsConsole             = "0.8.1"
  final val circeVersion            = "0.14.1"
  final val doobie                  = "1.0.0-RC2"
  final val fs2                     = "3.2.4"
  final val kafka                   = "2.8.1"
  final val logback                 = "1.2.10"
  final val log4j                   = "2.17.1"
  final val magnolia                = "1.0.0"
  final val monocle                 = "3.1.0"
  final val scala                   = "3.1.1"
  final val scalaParallelCollection = "1.0.4"
  final val scalaz                  = "7.2.29"
  final val scodec                  = "2.1.0"
  final val sttp                    = "3.4.1"
  final val xstream                 = "1.4.18"
  final val xml                     = "2.0.1"
  final val zio                     = "1.0.13"
  final val zioHttp                 = "2.0.0-RC2"
  final val zioPrelude              = "1.0.0-RC9"

  // ScalaJS
  final val scalaJsDom = "2.0.0"
  final val uTest      = "0.7.10"

  // Test
  final val mockito       = "1.10.19"
  final val munit         = "0.7.29"
  final val scalaTest     = "3.2.11"
  final val selenium      = "4.1.1"
  final val seleniumPlus  = "3.2.10.0"
  final val scalaCheck    = "1.15.4"
  final val testContainer = "0.40.0"
}

object Library {
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
  final val sttpBEZio               = "com.softwaremill.sttp.client3" %% "httpclient-backend-zio"           % Version.sttp
  final val sttpsSlf4j              = "com.softwaremill.sttp.client3" %% "slf4j-backend"                    % Version.sttp
  final val xstream                 = "com.thoughtworks.xstream"       % "xstream"                          % Version.xstream
  final val xml                     = "org.scala-lang.modules"        %% "scala-xml"                        % Version.xml
  final val zio                     = "dev.zio"                       %% "zio"                              % Version.zio
  final val zioStreams              = "dev.zio"                       %% "zio-streams"                      % Version.zio
  final val zioPrelude              = "dev.zio"                       %% "zio-prelude"                      % Version.zioPrelude
  final val zioHttp                 = "io.d11"                        %% "zhttp"                            % Version.zioHttp
  // test
  final val mockito                 = "org.mockito"                    % "mockito-all"                      % Version.mockito

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
    zio
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
