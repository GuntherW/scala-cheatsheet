import sbt.*

object Version {
  val akka                    = "2.8.5"
  val catsVersion             = "2.10.0"
  val catsConsole             = "0.8.1"
  val circeVersion            = "0.14.6"
  val cirisVersion            = "3.3.0"
  val doobie                  = "1.0.0-RC4"
  val ducktape                = "0.1.11"
  val fs2                     = "3.9.2"
  val gatling                 = "3.9.5"
  val h2                      = "2.2.224"
  val http4s                  = "0.23.23"
  val jackson                 = "2.15.3"
  val jwtHttp4s               = "1.2.1"
  val jwtScala                = "9.4.4"
  val kafka                   = "3.6.0"
  val logback                 = "1.4.11"
  val log4j                   = "2.21.0"
  val magnolia                = "1.3.3"
  val mongo                   = "4.11.0"
  val monocle                 = "3.2.0"
  val osLib                   = "0.9.1"
  val postgres                = "42.6.0"
  val quill                   = "4.8.0"
  val refined                 = "0.11.0"
  val resilience4j            = "2.1.0"
  val scala                   = "3.3.1"
  val scalaParallelCollection = "1.0.4"
  val scodec                  = "2.2.1"
  val sttp                    = "4.0.0-M6"
  val sttpOAuth               = "0.16.0"
  val sttpOpenAi              = "0.0.7"
  val squants                 = "1.8.3"
  val tyqu                    = "0.1.0"
  val xstream                 = "1.4.20"
  val xml                     = "2.2.0"
  val zio                     = "2.0.18"
  val zioJson                 = "0.6.2"
  val zioKafka                = "2.5.0"
  val zioHttp                 = "0.0.5"
  val zioPrelude              = "1.0.0-RC21"
  val zioSchema               = "0.4.14"
  val zioCli                  = "0.5.0"
  val zioOpenAI               = "0.2.1"

  // ScalaJS
  val scalaJsDom = "2.8.0"
  val uTest      = "0.8.1"

  // Test
  val cucumber       = "7.14.0"
  val cucumberScala  = "8.17.0"
  val junit          = "4.13.2"
  val junitInterface = "0.13.3"
  val mockito        = "5.6.0"
  val munit          = "1.0.0-M10"
  val scalaTest      = "3.2.17"
  val scalaCheck     = "1.17.0"
  val testContainer  = "0.41.0"
}

object Library {
  val akka                    = "com.typesafe.akka"             %% "akka-actor-typed"                 % Version.akka
  val catsCore                = "org.typelevel"                 %% "cats-core"                        % Version.catsVersion
  val catsFree                = "org.typelevel"                 %% "cats-free"                        % Version.catsVersion
  val circeCore               = "io.circe"                      %% "circe-core"                       % Version.circeVersion
  val circeGeneric            = "io.circe"                      %% "circe-generic"                    % Version.circeVersion
  val circeParser             = "io.circe"                      %% "circe-parser"                     % Version.circeVersion
  val ciris                   = "is.cir"                        %% "ciris"                            % Version.cirisVersion
  val cirisCirce              = "is.cir"                        %% "ciris-circe"                      % Version.cirisVersion
  val fs2Core                 = "co.fs2"                        %% "fs2-core"                         % Version.fs2
  val doobieCore              = "org.tpolecat"                  %% "doobie-core"                      % Version.doobie
  val doobiePostgres          = "org.tpolecat"                  %% "doobie-postgres"                  % Version.doobie
  val doobieHirari            = "org.tpolecat"                  %% "doobie-hikari"                    % Version.doobie
  val ducktape                = "io.github.arainko"             %% "ducktape"                         % Version.ducktape
  val fs2IO                   = "co.fs2"                        %% "fs2-io"                           % Version.fs2
  val fs2ReactiveStreams      = "co.fs2"                        %% "fs2-reactive-streams"             % Version.fs2
  val h2                      = "com.h2database"                 % "h2"                               % Version.h2
  val http4s                  = "org.http4s"                    %% "http4s-dsl"                       % Version.http4s
  val http4sEmberServer       = "org.http4s"                    %% "http4s-ember-server"              % Version.http4s
  val http4sEmberClient       = "org.http4s"                    %% "http4s-ember-client"              % Version.http4s
  val http4sDsl               = "org.http4s"                    %% "http4s-dsl"                       % Version.http4s
  val jacksonCore             = "com.fasterxml.jackson.core"     % "jackson-databind"                 % Version.jackson
  val jacksonScala            = "com.fasterxml.jackson.module"  %% "jackson-module-scala"             % Version.jackson
  val jwtHttp4s               = "dev.profunktor"                %% "http4s-jwt-auth"                  % Version.jwtHttp4s
  val jwtScala                = "com.github.jwt-scala"          %% "jwt-core"                         % Version.jwtScala
  val jwtCirce                = "com.github.jwt-scala"          %% "jwt-circe"                        % Version.jwtScala
  val kafkaClients            = "org.apache.kafka"               % "kafka-clients"                    % Version.kafka
  val kafkaStreams            = "org.apache.kafka"               % "kafka-streams"                    % Version.kafka
  val kafkaStreamsScala       = "org.apache.kafka"              %% "kafka-streams-scala"              % Version.kafka cross CrossVersion.for3Use2_13
  val logback                 = "ch.qos.logback"                 % "logback-classic"                  % Version.logback
  val log4jApi                = "org.apache.logging.log4j"       % "log4j-api"                        % Version.log4j
  val log4jCore               = "org.apache.logging.log4j"       % "log4j-core"                       % Version.log4j
  val log4jSlf4jImpl          = "org.apache.logging.log4j"       % "log4j-slf4j-impl"                 % Version.log4j
  val magnolia                = "com.softwaremill.magnolia1_3"  %% "magnolia"                         % Version.magnolia
  val mongoDriverJava         = "org.mongodb"                    % "mongodb-driver-sync"              % Version.mongo
  val monocle                 = "dev.optics"                    %% "monocle-core"                     % Version.monocle
  val osLib                   = "com.lihaoyi"                   %% "os-lib"                           % Version.osLib
  val postgres                = "org.postgresql"                 % "postgresql"                       % Version.postgres
  val quillJdbc               = "io.getquill"                   %% "quill-jdbc"                       % Version.quill
  val quillJdbcZio            = "io.getquill"                   %% "quill-jdbc-zio"                   % Version.quill
  val quillJasyncPostgres     = "io.getquill"                   %% "quill-jasync-postgres"            % Version.quill
  val refined                 = "eu.timepit"                    %% "refined"                          % Version.refined
  val resilience4j            = "io.github.resilience4j"         % "resilience4j-all"                 % Version.resilience4j
  val scalaParallelCollection = "org.scala-lang.modules"        %% "scala-parallel-collections"       % Version.scalaParallelCollection
  val scalaReflect            = "org.scala-lang"                 % "scala-reflect"                    % Version.scala % "provided"
  val scodec                  = "org.scodec"                    %% "scodec-core"                      % Version.scodec
  val sttpCore                = "com.softwaremill.sttp.client4" %% "core"                             % Version.sttp
  val sttpCirce               = "com.softwaremill.sttp.client4" %% "circe"                            % Version.sttp
  val sttpBEAsync             = "com.softwaremill.sttp.client4" %% "async-http-client-backend-future" % Version.sttp
  val sttpBEZio               = "com.softwaremill.sttp.client4" %% "zio"                              % Version.sttp
  val sttpsSlf4j              = "com.softwaremill.sttp.client4" %% "slf4j-backend"                    % Version.sttp
  val sttpOAuth               = "com.ocadotechnology"           %% "sttp-oauth2"                      % Version.sttpOAuth
  val sttpOpenAi              = "com.softwaremill.sttp.openai"  %% "core"                             % Version.sttpOpenAi
  val squants                 = "org.typelevel"                 %% "squants"                          % Version.squants
  val tyqu                    = "ch.epfl.tyqu"                  %% "tyqu"                             % Version.tyqu
  val xstream                 = "com.thoughtworks.xstream"       % "xstream"                          % Version.xstream
  val xml                     = "org.scala-lang.modules"        %% "scala-xml"                        % Version.xml
  val zio                     = "dev.zio"                       %% "zio"                              % Version.zio
  val zioKafka                = "dev.zio"                       %% "zio-kafka"                        % Version.zioKafka
  val zioStreams              = "dev.zio"                       %% "zio-streams"                      % Version.zio
  val zioJson                 = "dev.zio"                       %% "zio-json"                         % Version.zioJson
  val zioPrelude              = "dev.zio"                       %% "zio-prelude"                      % Version.zioPrelude
  val zioSchema               = "dev.zio"                       %% "zio-schema"                       % Version.zioSchema
  val zioSchemaJson           = "dev.zio"                       %% "zio-schema-json"                  % Version.zioSchema
  val zioSchemaProtobuf       = "dev.zio"                       %% "zio-schema-protobuf"              % Version.zioSchema
  val zioSchemaDerivation     = "dev.zio"                       %% "zio-schema-derivation"            % Version.zioSchema
  val zioHttp                 = "dev.zio"                       %% "zio-http"                         % Version.zioHttp
  val zioCli                  = "dev.zio"                       %% "zio-cli"                          % Version.zioCli
  val zioOpenAI               = "dev.zio"                       %% "zio-openai"                       % Version.zioOpenAI

  // test
  val cucumberScala   = "io.cucumber"          %% "cucumber-scala"                 % Version.cucumberScala
  val cucumberJunit   = "io.cucumber"           % "cucumber-junit"                 % Version.cucumber
  val cucumberPico    = "io.cucumber"           % "cucumber-picocontainer"         % Version.cucumber
  val junitInterface  = "com.github.sbt"        % "junit-interface"                % Version.junitInterface
  val junit           = "junit"                 % "junit"                          % Version.junit
  val gatlingCharts   = "io.gatling.highcharts" % "gatling-charts-highcharts"      % Version.gatling
  val gatling         = "io.gatling"            % "gatling-test-framework"         % Version.gatling
  val mockito         = "org.mockito"           % "mockito-core"                   % Version.mockito
  val munit           = "org.scalameta"        %% "munit"                          % Version.munit
  val munitScalaCheck = "org.scalameta"        %% "munit-scalacheck"               % Version.munit
  val scalatest       = "org.scalatest"        %% "scalatest"                      % Version.scalaTest
  val scalaCheck      = "org.scalacheck"       %% "scalacheck"                     % Version.scalaCheck
  val testContainer   = "com.dimafeng"         %% "testcontainers-scala-scalatest" % Version.testContainer
  val zioTest         = "dev.zio"              %% "zio-test"                       % Version.zio
  val zioTestSbt      = "dev.zio"              %% "zio-test-sbt"                   % Version.zio
  val zioTestJUnit    = "dev.zio"              %% "zio-test-junit"                 % Version.zio
}

object Dependencies {
  import Library.*
  val dependencies: Seq[ModuleID] = Seq(
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
    resilience4j,
    scalaParallelCollection,
    scodec,
    squants,
    xstream,
    xml,
    zio,
    jacksonCore,
    jacksonScala,
  )

  val testDependencies: Seq[ModuleID] = Seq(
    mockito       % Test,
    scalatest     % Test,
    scalaCheck    % Test,
    testContainer % Test,
  )
}
