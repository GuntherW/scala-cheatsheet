import sbt._

object Version {
  final val akka                    = "2.7.0"
  final val catsVersion             = "2.9.0"
  final val catsConsole             = "0.8.1"
  final val circeVersion            = "0.14.3"
  final val doobie                  = "1.0.0-RC2"
  final val ducktape                = "0.1.0"
  final val fs2                     = "3.3.0"
  final val gatling                 = "3.8.4"
  final val h2                      = "2.1.214"
  final val jackson                 = "2.14.0"
  final val kafka                   = "3.3.1"
  final val logback                 = "1.4.4"
  final val log4j                   = "2.19.0"
  final val magnolia                = "1.2.0"
  final val monocle                 = "3.1.0"
  final val postgres                = "42.5.0"
  final val quill                   = "4.6.0"
  final val refined                 = "0.10.1"
  final val scala                   = "3.2.1"
  final val scalaParallelCollection = "1.0.4"
  final val scodec                  = "2.2.0"
  final val sttp                    = "3.8.3"
  final val xstream                 = "1.4.19"
  final val xml                     = "2.1.0"
  final val zio                     = "2.0.3"
  final val zioJson                 = "0.3.0"
  final val zioKafka                = "2.0.1"
  final val zioHttp                 = "2.0.0-RC11"
  final val zioPrelude              = "1.0.0-RC16"

  // ScalaJS
  final val scalaJsDom = "2.3.0"
  final val uTest      = "0.8.1"

  // Test
  final val cucumber       = "7.9.0"
  final val cucumberScala  = "8.12.0"
  final val junit          = "4.13.2"
  final val junitInterface = "0.13.3"
  final val mockito        = "1.10.19"
  final val munit          = "1.0.0-M7"
  final val scalaTest      = "3.2.14"
  final val selenium       = "4.6.0"
  final val seleniumPlus   = "3.2.10.0"
  final val scalaCheck     = "1.17.0"
  final val testContainer  = "0.40.11"
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
  final val ducktape                = "io.github.arainko"             %% "ducktape"                         % Version.ducktape
  final val fs2IO                   = "co.fs2"                        %% "fs2-io"                           % Version.fs2
  final val fs2ReactiveStreams      = "co.fs2"                        %% "fs2-reactive-streams"             % Version.fs2
  final val h2                      = "com.h2database"                 % "h2"                               % Version.h2
  final val jacksonCore             = "com.fasterxml.jackson.core"     % "jackson-databind"                 % Version.jackson
  final val jacksonScala            = "com.fasterxml.jackson.module"  %% "jackson-module-scala"             % Version.jackson
  final val kafkaClients            = "org.apache.kafka"               % "kafka-clients"                    % Version.kafka
  final val kafkaStreams            = "org.apache.kafka"               % "kafka-streams"                    % Version.kafka
  final val kafkaStreamsScala       = "org.apache.kafka"              %% "kafka-streams-scala"              % Version.kafka cross CrossVersion.for3Use2_13
  final val logback                 = "ch.qos.logback"                 % "logback-classic"                  % Version.logback
  final val log4jApi                = "org.apache.logging.log4j"       % "log4j-api"                        % Version.log4j
  final val log4jCore               = "org.apache.logging.log4j"       % "log4j-core"                       % Version.log4j
  final val log4jSlf4jImpl          = "org.apache.logging.log4j"       % "log4j-slf4j-impl"                 % Version.log4j
  final val magnolia                = "com.softwaremill.magnolia1_3"  %% "magnolia"                         % Version.magnolia
  final val monocle                 = "dev.optics"                    %% "monocle-core"                     % Version.monocle
  final val postgres                = "org.postgresql"                 % "postgresql"                       % Version.postgres
  final val quillJdbc               = "io.getquill"                   %% "quill-jdbc"                       % Version.quill
  final val quillJdbcZio            = "io.getquill"                   %% "quill-jdbc-zio"                   % Version.quill
  final val quillJasyncPostgres     = "io.getquill"                   %% "quill-jasync-postgres"            % Version.quill
  final val refined                 = "eu.timepit"                    %% "refined"                          % Version.refined
  final val scalaParallelCollection = "org.scala-lang.modules"        %% "scala-parallel-collections"       % Version.scalaParallelCollection
  final val scodec                  = "org.scodec"                    %% "scodec-core"                      % Version.scodec
  final val sttpCore                = "com.softwaremill.sttp.client3" %% "core"                             % Version.sttp
  final val sttpCirce               = "com.softwaremill.sttp.client3" %% "circe"                            % Version.sttp
  final val sttpBEAsync             = "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % Version.sttp
  final val sttpBEZio               = "com.softwaremill.sttp.client3" %% "zio"                              % Version.sttp
  final val sttpsSlf4j              = "com.softwaremill.sttp.client3" %% "slf4j-backend"                    % Version.sttp
  final val xstream                 = "com.thoughtworks.xstream"       % "xstream"                          % Version.xstream
  final val xml                     = "org.scala-lang.modules"        %% "scala-xml"                        % Version.xml
  final val zio                     = "dev.zio"                       %% "zio"                              % Version.zio
  final val zioKafka                = "dev.zio"                       %% "zio-kafka"                        % Version.zioKafka
  final val zioStreams              = "dev.zio"                       %% "zio-streams"                      % Version.zio
  final val zioJson                 = "dev.zio"                       %% "zio-json"                         % Version.zioJson
  final val zioPrelude              = "dev.zio"                       %% "zio-prelude"                      % Version.zioPrelude
  final val zioHttp                 = "io.d11"                        %% "zhttp"                            % Version.zioHttp

  // test
  final val cucumberScala   = "io.cucumber"            %% "cucumber-scala"                 % Version.cucumberScala
  final val cucumberJunit   = "io.cucumber"             % "cucumber-junit"                 % Version.cucumber
  final val cucumberPico    = "io.cucumber"             % "cucumber-picocontainer"         % Version.cucumber
  final val junitInterface  = "com.github.sbt"          % "junit-interface"                % Version.junitInterface
  final val junit           = "junit"                   % "junit"                          % Version.junit
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
  final val zioHttpTest     = "io.d11"                 %% "zhttp-test"                     % "2.0.0-RC9"
}

object Dependencies {
  import Library._
  val dependencies = Seq(
    catsCore,
    catsFree,
    circeCore,
    circeGeneric,
    circeParser,
    ducktape,
    fs2Core,
    fs2IO,
    fs2ReactiveStreams,
    logback,
    monocle,
    refined,
    scalaParallelCollection,
    scodec,
    xstream,
    xml,
    zio,
    jacksonCore,
    jacksonScala,
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
