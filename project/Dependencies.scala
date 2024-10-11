import sbt.*

object Version {
  val akka                    = "2.8.7"
  val aws                     = "2.162.0"
  val caliban                 = "2.9.0"
  val catsEffectCps           = "0.4.0"
  val catsVersion             = "2.12.0"
  val catsConsole             = "0.8.1"
  val circeVersion            = "0.14.10"
  val cirisVersion            = "3.6.0"
  val doobie                  = "1.0.0-RC6"
  val ducktape                = "0.2.5"
  val fs2                     = "3.11.0"
  val gatling                 = "3.12.0"
  val gears                   = "0.2.0"
  val h2                      = "2.3.232"
  val http4s                  = "0.23.28"
  val jackson                 = "2.18.0"
  val jwtHttp4s               = "1.2.3"
  val jwtScala                = "10.0.1"
  val jsonSchemaValidator     = "1.5.2"
  val kafka                   = "3.8.0"
  val kyo                     = "0.12.2"
  val logback                 = "1.5.9"
  val log4j                   = "2.24.1"
  val macwire                 = "2.6.4"
  val magnolia                = "1.3.7"
  val mongo                   = "4.10.0"
  val monocle                 = "3.3.0"
  val neotype                 = "0.3.6"
  val osLib                   = "0.11.1"
  val ox                      = "0.5.1"
  val postgres                = "42.7.4"
  val pureConfig              = "0.17.7"
  val pulsar4s                = "2.10.0"
  val quill                   = "4.8.5"
  val reactiveMongo           = "1.1.0-pekko.noshaded.RC13"
  val refined                 = "0.11.2"
  val resilience4j            = "2.2.0"
  val scala                   = "3.5.1"
  val scalaParallelCollection = "1.0.4"
  val scodec                  = "2.3.2"
  val skunk                   = "0.6.4"
  val sttp                    = "4.0.0-M18"
  val sttpOAuth               = "0.18.0"
  val sttpOpenAi              = "0.2.3"
  val squants                 = "1.8.3"
  val tapir                   = "1.11.7"
  val tyqu                    = "0.1.0"
  val xstream                 = "1.4.20"
  val xml                     = "2.3.0"
  val zio                     = "2.1.11"
  val zioJson                 = "0.7.3"
  val zioKafka                = "2.8.2"
  val zioHttp                 = "3.0.1"
  val zioPrelude              = "1.0.0-RC31"
  val zioSchema               = "1.5.0"
  val zioCli                  = "0.5.0"
  val zioOpenAI               = "0.4.1"

  // ScalaJS
  val scalaJsDom = "2.8.0"
  val uTest      = "0.8.4"

  // Test
  val cucumber       = "7.20.1"
  val cucumberScala  = "8.25.1"
  val junit          = "4.13.2"
  val junitInterface = "0.13.3"
  val mockito        = "5.14.1"
  val munit          = "1.0.0"
  val scalaTest      = "3.2.19"
  val scalaCheck     = "1.18.1"
  val testContainer  = "0.41.4"
  val weaverCats     = "0.8.4"
}

object Library {
  val akka                    = "com.typesafe.akka"             %% "akka-actor-typed"                 % Version.akka
  val awsCdk                  = "software.amazon.awscdk"         % "aws-cdk-lib"                      % Version.aws
  val calibanQuick            = "com.github.ghostdogpr"         %% "caliban-quick"                    % Version.caliban
  val calibanClient           = "com.github.ghostdogpr"         %% "caliban-client"                   % Version.caliban
  val catsCore                = "org.typelevel"                 %% "cats-core"                        % Version.catsVersion
  val catsEffectCps           = "org.typelevel"                 %% "cats-effect-cps"                  % Version.catsEffectCps
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
  val gears                   = "ch.epfl.lamp"                  %% "gears"                            % Version.gears
  val h2                      = "com.h2database"                 % "h2"                               % Version.h2
  val http4s                  = "org.http4s"                    %% "http4s-dsl"                       % Version.http4s
  val http4sEmberServer       = "org.http4s"                    %% "http4s-ember-server"              % Version.http4s
  val http4sBlazeServer       = "org.http4s"                    %% "http4s-blaze-server"              % "0.23.16"
  val http4sEmberClient       = "org.http4s"                    %% "http4s-ember-client"              % Version.http4s
  val http4sCirce             = "org.http4s"                    %% "http4s-circe"                     % Version.http4s
  val http4sDsl               = "org.http4s"                    %% "http4s-dsl"                       % Version.http4s
  val jacksonCore             = "com.fasterxml.jackson.core"     % "jackson-databind"                 % Version.jackson
  val jacksonScala            = "com.fasterxml.jackson.module"  %% "jackson-module-scala"             % Version.jackson
  val jsonSchemaValidator     = "com.networknt"                  % "json-schema-validator"            % Version.jsonSchemaValidator
  val jwtHttp4s               = "dev.profunktor"                %% "http4s-jwt-auth"                  % Version.jwtHttp4s
  val jwtScala                = "com.github.jwt-scala"          %% "jwt-core"                         % Version.jwtScala
  val jwtCirce                = "com.github.jwt-scala"          %% "jwt-circe"                        % Version.jwtScala
  val kafkaClients            = "org.apache.kafka"               % "kafka-clients"                    % Version.kafka
  val kafkaStreams            = "org.apache.kafka"               % "kafka-streams"                    % Version.kafka
  val kafkaStreamsScala       = "org.apache.kafka"              %% "kafka-streams-scala"              % Version.kafka cross CrossVersion.for3Use2_13
  val kyoCore                 = "io.getkyo"                     %% "kyo-core"                         % Version.kyo
  val kyoDirect               = "io.getkyo"                     %% "kyo-direct"                       % Version.kyo
  val kyoCache                = "io.getkyo"                     %% "kyo-cache"                        % Version.kyo
  val kyoStat                 = "io.getkyo"                     %% "kyo-stats-otel"                   % Version.kyo
  val kyoSttp                 = "io.getkyo"                     %% "kyo-sttp"                         % Version.kyo
  val kyoTapir                = "io.getkyo"                     %% "kyo-tapir"                        % Version.kyo
  val logback                 = "ch.qos.logback"                 % "logback-classic"                  % Version.logback
  val log4jApi                = "org.apache.logging.log4j"       % "log4j-api"                        % Version.log4j
  val log4jCore               = "org.apache.logging.log4j"       % "log4j-core"                       % Version.log4j
  val log4jSlf4jImpl          = "org.apache.logging.log4j"       % "log4j-slf4j-impl"                 % Version.log4j
  val macwire                 = "com.softwaremill.macwire"      %% "macros"                           % Version.macwire
  val magnolia                = "com.softwaremill.magnolia1_3"  %% "magnolia"                         % Version.magnolia
  val mongoDriverJava         = "org.mongodb"                    % "mongodb-driver-sync"              % Version.mongo
  val monocle                 = "dev.optics"                    %% "monocle-core"                     % Version.monocle
  val neotype                 = "io.github.kitlangton"          %% "neotype"                          % Version.neotype
  val osLib                   = "com.lihaoyi"                   %% "os-lib"                           % Version.osLib
  val ox                      = "com.softwaremill.ox"           %% "core"                             % Version.ox
  val postgres                = "org.postgresql"                 % "postgresql"                       % Version.postgres
  val pureConfig              = "com.github.pureconfig"         %% "pureconfig-generic-scala3"        % Version.pureConfig
  val pulsar4s                = "com.clever-cloud.pulsar4s"     %% "pulsar4s-core"                    % Version.pulsar4s
  val pulsar4sCirce           = "com.clever-cloud.pulsar4s"     %% "pulsar4s-circe"                   % Version.pulsar4s
  val quillJdbc               = "io.getquill"                   %% "quill-jdbc"                       % Version.quill
  val quillJdbcZio            = "io.getquill"                   %% "quill-jdbc-zio"                   % Version.quill
  val reactiveMongo           = "org.reactivemongo"             %% "reactivemongo"                    % Version.reactiveMongo
  val refined                 = "eu.timepit"                    %% "refined"                          % Version.refined
  val resilience4j            = "io.github.resilience4j"         % "resilience4j-all"                 % Version.resilience4j
  val scalaParallelCollection = "org.scala-lang.modules"        %% "scala-parallel-collections"       % Version.scalaParallelCollection
  val scalaReflect            = "org.scala-lang"                 % "scala-reflect"                    % Version.scala % "provided"
  val scodec                  = "org.scodec"                    %% "scodec-core"                      % Version.scodec
  val skunk                   = "org.tpolecat"                  %% "skunk-core"                       % Version.skunk
  val sttpCore                = "com.softwaremill.sttp.client4" %% "core"                             % Version.sttp
  val sttpCirce               = "com.softwaremill.sttp.client4" %% "circe"                            % Version.sttp
  val sttpBEAsync             = "com.softwaremill.sttp.client4" %% "async-http-client-backend-future" % Version.sttp
  val sttpBEZio               = "com.softwaremill.sttp.client4" %% "zio"                              % Version.sttp
  val sttpsSlf4j              = "com.softwaremill.sttp.client4" %% "slf4j-backend"                    % Version.sttp
  val sttpOAuth               = "com.ocadotechnology"           %% "sttp-oauth2"                      % Version.sttpOAuth
  val sttpOpenAi              = "com.softwaremill.sttp.openai"  %% "core"                             % Version.sttpOpenAi
  val squants                 = "org.typelevel"                 %% "squants"                          % Version.squants
  val tapirAwsLambda          = "com.softwaremill.sttp.tapir"   %% "tapir-aws-lambda"                 % Version.tapir
  val tapirAwsCdk             = "com.softwaremill.sttp.tapir"   %% "tapir-aws-cdk"                    % Version.tapir
  val tapirAwsSam             = "com.softwaremill.sttp.tapir"   %% "tapir-aws-sam"                    % Version.tapir
  val tapirHttp4sServer       = "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server"              % Version.tapir
  val tapirJsonCirce          = "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"                 % Version.tapir
  val tapirJdkHttp            = "com.softwaremill.sttp.tapir"   %% "tapir-jdkhttp-server"             % Version.tapir
  val tapirNettyFuture        = "com.softwaremill.sttp.tapir"   %% "tapir-netty-server"               % Version.tapir
  val tapirNettyServerSync    = "com.softwaremill.sttp.tapir"   %% "tapir-netty-server-sync"          % Version.tapir
  val tapirSwaggerUiBundle    = "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"          % Version.tapir
  val tapirPrometheusMetrics  = "com.softwaremill.sttp.tapir"   %% "tapir-prometheus-metrics"         % Version.tapir
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
  val zioSchemaBson           = "dev.zio"                       %% "zio-schema-bson"                  % Version.zioSchema
  val zioSchemaProtobuf       = "dev.zio"                       %% "zio-schema-protobuf"              % Version.zioSchema
  val zioSchemaDerivation     = "dev.zio"                       %% "zio-schema-derivation"            % Version.zioSchema
  val zioHttp                 = "dev.zio"                       %% "zio-http"                         % Version.zioHttp
  val zioCli                  = "dev.zio"                       %% "zio-cli"                          % Version.zioCli
  val zioOpenAI               = "dev.zio"                       %% "zio-openai"                       % Version.zioOpenAI

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
  val munitScalaCheck     = "org.scalameta"               %% "munit-scalacheck"               % Version.munit
  val scalatest           = "org.scalatest"               %% "scalatest"                      % Version.scalaTest
  val scalaCheck          = "org.scalacheck"              %% "scalacheck"                     % Version.scalaCheck
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
    catsEffectCps,
    catsFree,
    circeCore,
    circeGeneric,
    circeParser,
    ducktape,
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
    testContainer % Test,
  )
}
