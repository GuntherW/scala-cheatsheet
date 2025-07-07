import sbt.*

object Version {
  val akka                    = "2.8.8"
  val aws                     = "2.203.1"
  val avro4s                  = "5.0.14"
  val borer                   = "1.16.1"
  val caliban                 = "2.10.1"
  val catsEffect              = "3.6.1"
  val catsEffectCps           = "0.4.0"
  val catsVersion             = "2.13.0"
  val catsConsole             = "0.8.1"
  val circeVersion            = "0.14.14"
  val cirisVersion            = "3.9.0"
  val chimney                 = "1.8.1"
  val doobie                  = "1.0.0-RC9"
  val ducktape                = "0.2.9"
  val duckdb                  = "1.3.1.0"
  val fs2                     = "3.12.0"
  val gatling                 = "3.14.3"
  val gears                   = "0.2.0"
  val h2                      = "2.3.232"
  val http4s                  = "0.23.30"
  val jackson                 = "2.19.1"
  val jwtHttp4s               = "2.0.8"
  val jwtScala                = "10.0.4"
  val jsonSchemaValidator     = "1.5.8"
  val jsoniter                = "2.36.6"
  val kafka                   = "4.0.0"
  val kyo                     = "0.19.0"
  val logback                 = "1.5.18"
  val log4j                   = "2.25.0"
  val macwire                 = "2.6.6"
  val magnolia                = "1.3.18"
  val magnum                  = "2.0.0-M2"
  val mongo                   = "5.5.1"
  val monocle                 = "3.3.0"
  val neotype                 = "0.3.25"
  val openTelemetry           = "1.51.0"
  val osLib                   = "0.11.4"
  val ox                      = "0.7.0"
  val parserCombinators       = "2.4.0"
  val postgres                = "42.7.7"
  val pureConfig              = "0.17.9"
  val pulsar4s                = "2.12.0.1"
  val quill                   = "4.8.6"
  val refined                 = "0.11.3"
  val resilience4j            = "2.3.0"
  val scala                   = "3.7.1"
  val scalaParallelCollection = "1.2.0"
  val scalasql                = "0.1.20"
  val scodec                  = "2.3.2"
  val skunk                   = "0.6.4"
  val sttp                    = "4.0.9"
  val sttpOAuth               = "0.18.0"
  val sttpOpenAi              = "0.3.6"
  val squants                 = "1.8.3"
  val tapir                   = "1.11.35"
  val toml                    = "0.3.0"
  val tyqu                    = "0.1.0"
  val upickle                 = "4.2.1"
  val xstream                 = "1.4.21"
  val xml                     = "2.4.0"
  val zio                     = "2.1.19"
  val zioJson                 = "0.7.44"
  val zioKafka                = "3.0.0"
  val zioHttp                 = "3.3.3"
  val zioPrelude              = "1.0.0-RC41"
  val zioSchema               = "1.7.3"
  val zioCli                  = "0.7.2"
  val zioOpenAI               = "0.4.1"

  // ScalaJS
  val scalaJsDom = "2.8.0"
  val uTest      = "0.8.9"

  // Test
  val cucumber        = "7.23.0"
  val cucumberScala   = "8.28.0"
  val junit           = "4.13.2"
  val junitInterface  = "0.13.3"
  val mockito         = "5.18.0"
  val munit           = "1.1.1"
  val munitScalaCheck = "1.1.0"
  val scalaCheck      = "1.18.1"
  val scalaMock       = "7.4.0"
  val scalaTest       = "3.2.19"
  val testContainer   = "0.43.0"
  val weaverCats      = "0.8.4"
}

object Library {
  val akka                      = "com.typesafe.akka"                     %% "akka-actor-typed"                          % Version.akka
  val awsCdk                    = "software.amazon.awscdk"                 % "aws-cdk-lib"                               % Version.aws
  val avro4s                    = "com.sksamuel.avro4s"                   %% "avro4s-core"                               % Version.avro4s
  val borerCore                 = "io.bullet"                             %% "borer-core"                                % Version.borer
  val borerDerivation           = "io.bullet"                             %% "borer-derivation"                          % Version.borer
  val borerCirce                = "io.bullet"                             %% "borer-compat-circe"                        % Version.borer
  val borerScodec               = "io.bullet"                             %% "borer-compat-scodec"                       % Version.borer
  val calibanQuick              = "com.github.ghostdogpr"                 %% "caliban-quick"                             % Version.caliban
  val calibanClient             = "com.github.ghostdogpr"                 %% "caliban-client"                            % Version.caliban
  val catsCore                  = "org.typelevel"                         %% "cats-core"                                 % Version.catsVersion
  val catsEffect                = "org.typelevel"                         %% "cats-effect"                               % Version.catsEffect
  val catsEffectCps             = "org.typelevel"                         %% "cats-effect-cps"                           % Version.catsEffectCps
  val catsFree                  = "org.typelevel"                         %% "cats-free"                                 % Version.catsVersion
  val circeCore                 = "io.circe"                              %% "circe-core"                                % Version.circeVersion
  val circeGeneric              = "io.circe"                              %% "circe-generic"                             % Version.circeVersion
  val circeParser               = "io.circe"                              %% "circe-parser"                              % Version.circeVersion
  val ciris                     = "is.cir"                                %% "ciris"                                     % Version.cirisVersion
  val cirisCirce                = "is.cir"                                %% "ciris-circe"                               % Version.cirisVersion
  val chimney                   = "io.scalaland"                          %% "chimney"                                   % Version.chimney
  val doobieCore                = "org.tpolecat"                          %% "doobie-core"                               % Version.doobie
  val doobiePostgres            = "org.tpolecat"                          %% "doobie-postgres"                           % Version.doobie
  val doobieHirari              = "org.tpolecat"                          %% "doobie-hikari"                             % Version.doobie
  val ducktape                  = "io.github.arainko"                     %% "ducktape"                                  % Version.ducktape
  val duckdb                    = "org.duckdb"                             % "duckdb_jdbc"                               % Version.duckdb
  val fs2Core                   = "co.fs2"                                %% "fs2-core"                                  % Version.fs2
  val fs2IO                     = "co.fs2"                                %% "fs2-io"                                    % Version.fs2
  val fs2ReactiveStreams        = "co.fs2"                                %% "fs2-reactive-streams"                      % Version.fs2
  val gears                     = "ch.epfl.lamp"                          %% "gears"                                     % Version.gears
  val h2                        = "com.h2database"                         % "h2"                                        % Version.h2
  val http4s                    = "org.http4s"                            %% "http4s-dsl"                                % Version.http4s
  val http4sEmberServer         = "org.http4s"                            %% "http4s-ember-server"                       % Version.http4s
  val http4sEmberClient         = "org.http4s"                            %% "http4s-ember-client"                       % Version.http4s
  val http4sCirce               = "org.http4s"                            %% "http4s-circe"                              % Version.http4s
  val http4sDsl                 = "org.http4s"                            %% "http4s-dsl"                                % Version.http4s
  val jacksonCore               = "com.fasterxml.jackson.core"             % "jackson-databind"                          % Version.jackson
  val jacksonScala              = "com.fasterxml.jackson.module"          %% "jackson-module-scala"                      % Version.jackson
  val jsoniter                  = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"                       % Version.jsoniter
  val jsoniterMacros            = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros"                     % Version.jsoniter
  val jsonSchemaValidator       = "com.networknt"                          % "json-schema-validator"                     % Version.jsonSchemaValidator
  val jwtHttp4s                 = "dev.profunktor"                        %% "http4s-jwt-auth"                           % Version.jwtHttp4s
  val jwtScala                  = "com.github.jwt-scala"                  %% "jwt-core"                                  % Version.jwtScala
  val jwtCirce                  = "com.github.jwt-scala"                  %% "jwt-circe"                                 % Version.jwtScala
  val kafkaClients              = "org.apache.kafka"                       % "kafka-clients"                             % Version.kafka
  val kafkaStreams              = "org.apache.kafka"                       % "kafka-streams"                             % Version.kafka
  val kafkaStreamsScala         = "org.apache.kafka"                      %% "kafka-streams-scala"                       % Version.kafka cross CrossVersion.for3Use2_13
  val kyoCore                   = "io.getkyo"                             %% "kyo-core"                                  % Version.kyo
  val kyoDirect                 = "io.getkyo"                             %% "kyo-direct"                                % Version.kyo
  val kyoCache                  = "io.getkyo"                             %% "kyo-cache"                                 % Version.kyo
  val kyoStat                   = "io.getkyo"                             %% "kyo-stats-otel"                            % Version.kyo
  val kyoSttp                   = "io.getkyo"                             %% "kyo-sttp"                                  % Version.kyo
  val kyoTapir                  = "io.getkyo"                             %% "kyo-tapir"                                 % Version.kyo
  val logback                   = "ch.qos.logback"                         % "logback-classic"                           % Version.logback
  val log4jApi                  = "org.apache.logging.log4j"               % "log4j-api"                                 % Version.log4j
  val log4jCore                 = "org.apache.logging.log4j"               % "log4j-core"                                % Version.log4j
  val log4jSlf4jImpl            = "org.apache.logging.log4j"               % "log4j-slf4j-impl"                          % Version.log4j
  val macwire                   = "com.softwaremill.macwire"              %% "macros"                                    % Version.macwire
  val magnolia                  = "com.softwaremill.magnolia1_3"          %% "magnolia"                                  % Version.magnolia
  val magnum                    = "com.augustnagro"                       %% "magnum"                                    % Version.magnum
  val magnumpg                  = "com.augustnagro"                       %% "magnumpg"                                  % Version.magnum
  val mongoDriverSync           = "org.mongodb"                            % "mongodb-driver-sync"                       % Version.mongo
  val monocle                   = "dev.optics"                            %% "monocle-core"                              % Version.monocle
  val neotype                   = "io.github.kitlangton"                  %% "neotype"                                   % Version.neotype
  val openTelemtry              = "io.opentelemetry"                       % "opentelemetry-exporter-otlp"               % Version.openTelemetry
  val openTelemtryAutoConfigure = "io.opentelemetry"                       % "opentelemetry-sdk-extension-autoconfigure" % Version.openTelemetry
  val osLib                     = "com.lihaoyi"                           %% "os-lib"                                    % Version.osLib
  val ox                        = "com.softwaremill.ox"                   %% "core"                                      % Version.ox
  val parserCombinators         = "org.scala-lang.modules"                %% "scala-parser-combinators"                  % Version.parserCombinators
  val postgres                  = "org.postgresql"                         % "postgresql"                                % Version.postgres
  val pureConfig                = "com.github.pureconfig"                 %% "pureconfig-generic-scala3"                 % Version.pureConfig
  val pulsar4s                  = "com.clever-cloud.pulsar4s"             %% "pulsar4s-core"                             % Version.pulsar4s
  val pulsar4sCirce             = "com.clever-cloud.pulsar4s"             %% "pulsar4s-circe"                            % Version.pulsar4s
  val quillJdbc                 = "io.getquill"                           %% "quill-jdbc"                                % Version.quill
  val quillJdbcZio              = "io.getquill"                           %% "quill-jdbc-zio"                            % Version.quill
  val refined                   = "eu.timepit"                            %% "refined"                                   % Version.refined
  val resilience4j              = "io.github.resilience4j"                 % "resilience4j-all"                          % Version.resilience4j
  val scalaParallelCollection   = "org.scala-lang.modules"                %% "scala-parallel-collections"                % Version.scalaParallelCollection
  val scalasql                  = "com.lihaoyi"                           %% "scalasql"                                  % Version.scalasql
  val scalasqlNamedTuples       = "com.lihaoyi"                           %% "scalasql-namedtuples"                      % Version.scalasql
  val scodec                    = "org.scodec"                            %% "scodec-core"                               % Version.scodec
  val skunk                     = "org.tpolecat"                          %% "skunk-core"                                % Version.skunk
  val sttpCore                  = "com.softwaremill.sttp.client4"         %% "core"                                      % Version.sttp
  val sttpCirce                 = "com.softwaremill.sttp.client4"         %% "circe"                                     % Version.sttp
  val sttpBEZio                 = "com.softwaremill.sttp.client4"         %% "zio"                                       % Version.sttp
  val sttpFs2                   = "com.softwaremill.sttp.client4"         %% "fs2"                                       % Version.sttp
  val sttpJsoniter              = "com.softwaremill.sttp.client4"         %% "jsoniter"                                  % Version.sttp
  val sttpsSlf4j                = "com.softwaremill.sttp.client4"         %% "slf4j-backend"                             % Version.sttp
  val sttpsOpenTelemetry        = "com.softwaremill.sttp.client4"         %% "opentelemetry-backend"                     % Version.sttp
  val sttpOAuth                 = "com.ocadotechnology"                   %% "sttp-oauth2"                               % Version.sttpOAuth
  val sttpOpenAi                = "com.softwaremill.sttp.openai"          %% "core"                                      % Version.sttpOpenAi
  val squants                   = "org.typelevel"                         %% "squants"                                   % Version.squants
  val tapirAwsLambda            = "com.softwaremill.sttp.tapir"           %% "tapir-aws-lambda"                          % Version.tapir
  val tapirAwsCdk               = "com.softwaremill.sttp.tapir"           %% "tapir-aws-cdk"                             % Version.tapir
  val tapirAwsSam               = "com.softwaremill.sttp.tapir"           %% "tapir-aws-sam"                             % Version.tapir
  val tapirHttp4sServer         = "com.softwaremill.sttp.tapir"           %% "tapir-http4s-server"                       % Version.tapir
  val tapirJsonCirce            = "com.softwaremill.sttp.tapir"           %% "tapir-json-circe"                          % Version.tapir
  val tapirJdkHttp              = "com.softwaremill.sttp.tapir"           %% "tapir-jdkhttp-server"                      % Version.tapir
  val tapirNettyFuture          = "com.softwaremill.sttp.tapir"           %% "tapir-netty-server"                        % Version.tapir
  val tapirNettyServerSync      = "com.softwaremill.sttp.tapir"           %% "tapir-netty-server-sync"                   % Version.tapir
  val tapirSwaggerUiBundle      = "com.softwaremill.sttp.tapir"           %% "tapir-swagger-ui-bundle"                   % Version.tapir
  val tapirPrometheusMetrics    = "com.softwaremill.sttp.tapir"           %% "tapir-prometheus-metrics"                  % Version.tapir
  val toml                      = "com.indoorvivants"                     %% "toml"                                      % Version.toml
  val tyqu                      = "ch.epfl.tyqu"                          %% "tyqu"                                      % Version.tyqu
  val upickle                   = "com.lihaoyi"                           %% "upickle"                                   % Version.upickle
  val xstream                   = "com.thoughtworks.xstream"               % "xstream"                                   % Version.xstream
  val xml                       = "org.scala-lang.modules"                %% "scala-xml"                                 % Version.xml
  val zio                       = "dev.zio"                               %% "zio"                                       % Version.zio
  val zioKafka                  = "dev.zio"                               %% "zio-kafka"                                 % Version.zioKafka
  val zioStreams                = "dev.zio"                               %% "zio-streams"                               % Version.zio
  val zioJson                   = "dev.zio"                               %% "zio-json"                                  % Version.zioJson
  val zioPrelude                = "dev.zio"                               %% "zio-prelude"                               % Version.zioPrelude
  val zioSchema                 = "dev.zio"                               %% "zio-schema"                                % Version.zioSchema
  val zioSchemaJson             = "dev.zio"                               %% "zio-schema-json"                           % Version.zioSchema
  val zioSchemaBson             = "dev.zio"                               %% "zio-schema-bson"                           % Version.zioSchema
  val zioSchemaProtobuf         = "dev.zio"                               %% "zio-schema-protobuf"                       % Version.zioSchema
  val zioSchemaDerivation       = "dev.zio"                               %% "zio-schema-derivation"                     % Version.zioSchema
  val zioHttp                   = "dev.zio"                               %% "zio-http"                                  % Version.zioHttp
  val zioCli                    = "dev.zio"                               %% "zio-cli"                                   % Version.zioCli
  val zioOpenAI                 = "dev.zio"                               %% "zio-openai"                                % Version.zioOpenAI

  // test
  val cucumberScala       = "io.cucumber"                 %% "cucumber-scala"                 % Version.cucumberScala
  val cucumberJunit       = "io.cucumber"                  % "cucumber-junit"                 % Version.cucumber
  val cucumberPico        = "io.cucumber"                  % "cucumber-picocontainer"         % Version.cucumber
  val junitInterface      = "com.github.sbt"               % "junit-interface"                % Version.junitInterface
  val junit               = "junit"                        % "junit"                          % Version.junit
  val gatlingCharts       = "io.gatling.highcharts"        % "gatling-charts-highcharts"      % Version.gatling
  val gatling             = "io.gatling"                   % "gatling-test-framework"         % Version.gatling
  val mockito             = "org.mockito"                  % "mockito-core"                   % Version.mockito
  val munit               = "org.scalameta"               %% "munit"                          % Version.munit
  val munitScalaCheck     = "org.scalameta"               %% "munit-scalacheck"               % Version.munitScalaCheck
  val scalaCheck          = "org.scalacheck"              %% "scalacheck"                     % Version.scalaCheck
  val scalaMock           = "org.scalamock"               %% "scalamock"                      % Version.scalaMock
  val scalatest           = "org.scalatest"               %% "scalatest"                      % Version.scalaTest
  val tapirSttpStubServer = "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"         % Version.tapir
  val testContainer       = "com.dimafeng"                %% "testcontainers-scala-scalatest" % Version.testContainer
  val testContainerMongo  = "com.dimafeng"                %% "testcontainers-scala-mongodb"   % Version.testContainer
  val weaverCats          = "com.disneystreaming"         %% "weaver-cats"                    % Version.weaverCats
  val zioTest             = "dev.zio"                     %% "zio-test"                       % Version.zio
  val zioTestSbt          = "dev.zio"                     %% "zio-test-sbt"                   % Version.zio
  val zioTestJUnit        = "dev.zio"                     %% "zio-test-junit"                 % Version.zio
}

object Dependencies {
  import Library.*
  val dependencies: Seq[ModuleID] = Seq(
    catsCore,
    catsEffect,
    catsEffectCps,
    catsFree,
    circeCore,
    circeGeneric,
    circeParser,
    fs2Core,
    fs2IO,
    fs2ReactiveStreams,
    jsonSchemaValidator,
    logback,
    monocle,
    neotype,
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
    scalaMock     % Test,
    testContainer % Test,
  )
}
