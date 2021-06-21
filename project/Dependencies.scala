import sbt._

object Version {
  final val akkaVersion             = "2.6.15"
  final val catsVersion             = "2.6.1"
  final val catsConsole             = "0.8.1"
  final val chimney                 = "0.6.1"
  final val circeVersion            = "0.14.1"
  final val enumeratum              = "1.7.0"
  final val fs2                     = "2.5.6"
  final val kittensVersion          = "2.3.2"
  final val logback                 = "1.2.3"
  final val magnolia                = "0.17.0"
  final val magnolia3               = "2.0.0-M8"
  final val monix                   = "3.4.0"
  final val monocleVersion          = "2.1.0"
  final val refined                 = "0.9.26"
  final val scala2                  = "2.13.6"
  final val scala                   = "3.0.0"
  final val scalaMeta               = "4.4.21"
  final val scalaParallelCollection = "1.0.3"
  final val scalaz                  = "7.2.29"
  final val scodec                  = "1.11.8"
  final val simulacrum              = "0.19.0"
  final val shapeless               = "2.3.7"
  final val sttp                    = "3.3.7"
  final val xstream                 = "1.4.17"
  final val xml                     = "2.0.0-M5"
  final val zio                     = "1.0.9"

  // ScalaJS
  final val scalaJsDom = "1.1.0"
  final val uTest      = "0.7.10"

  // Test
  final val mockito             = "1.10.19"
  final val munit               = "0.7.26"
  final val scalaTest           = "3.2.9"
  final val selenium            = "3.141.59"
  final val seleniumPlus        = "3.2.9.0"
  final val scalaCheck          = "1.15.4"
  final val scalaCheckShapeless = "1.3.0"
}

object Library {
  final val akkaStream              = "com.typesafe.akka"             %% "akka-stream"                      % Version.akkaVersion cross CrossVersion.for3Use2_13
  final val catsCore                = "org.typelevel"                 %% "cats-core"                        % Version.catsVersion
  final val catsFree                = "org.typelevel"                 %% "cats-free"                        % Version.catsVersion
  final val chimney                 = "io.scalaland"                  %% "chimney"                          % Version.chimney cross CrossVersion.for3Use2_13
  final val circeCore               = "io.circe"                      %% "circe-core"                       % Version.circeVersion
  final val circeGeneric            = "io.circe"                      %% "circe-generic"                    % Version.circeVersion
  final val circeGenericExtras      = "io.circe"                      %% "circe-generic-extras"             % Version.circeVersion cross CrossVersion.for3Use2_13
  final val circeParser             = "io.circe"                      %% "circe-parser"                     % Version.circeVersion
  final val circeLiteral            = "io.circe"                      %% "circe-literal"                    % Version.circeVersion cross CrossVersion.for3Use2_13
  final val enumeratum              = "com.beachape"                  %% "enumeratum"                       % Version.enumeratum cross CrossVersion.for3Use2_13
  final val fs2Core                 = "co.fs2"                        %% "fs2-core"                         % Version.fs2
  final val fs2IO                   = "co.fs2"                        %% "fs2-io"                           % Version.fs2 // optional I/O library
  final val fs2ReactiveStreams      = "co.fs2"                        %% "fs2-reactive-streams"             % Version.fs2 // optional reactive streams interop
  final val kittens                 = "org.typelevel"                 %% "kittens"                          % Version.kittensVersion cross CrossVersion.for3Use2_13
  final val logback                 = "ch.qos.logback"                 % "logback-classic"                  % Version.logback
  final val magnolia                = "com.propensive"                %% "magnolia"                         % Version.magnolia
  final val magnolia3               = "com.softwaremill.magnolia"     %% "magnolia-core"                    % Version.magnolia3
  final val monix                   = "io.monix"                      %% "monix"                            % Version.monix
  final val monocle                 = "com.github.julien-truffaut"    %% "monocle-state"                    % Version.monocleVersion cross CrossVersion.for3Use2_13
  final val refined                 = "eu.timepit"                    %% "refined"                          % Version.refined
  final val scalaMeta               = "org.scalameta"                 %% "scalameta"                        % Version.scalaMeta cross CrossVersion.for3Use2_13
  final val scalaParallelCollection = "org.scala-lang.modules"        %% "scala-parallel-collections"       % Version.scalaParallelCollection
  final val scalaReflect            = "org.scala-lang"                 % "scala-reflect"                    % Version.scala2
  final val shapeless               = "com.chuusai"                   %% "shapeless"                        % Version.shapeless cross CrossVersion.for3Use2_13
  final val simulacrum              = "com.github.mpilquist"          %% "simulacrum"                       % Version.simulacrum cross CrossVersion.for3Use2_13
  final val scodec                  = "org.scodec"                    %% "scodec-core"                      % Version.scodec cross CrossVersion.for3Use2_13
  final val sttpCore                = "com.softwaremill.sttp.client3" %% "core"                             % Version.sttp
  final val sttpCirce               = "com.softwaremill.sttp.client3" %% "circe"                            % Version.sttp
  final val sttpBEAsync             = "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % Version.sttp cross CrossVersion.for3Use2_13
  final val sttpBEZio               = "com.softwaremill.sttp.client3" %% "httpclient-backend-zio"           % Version.sttp
  final val sttpBEAkkaHttp          = "com.softwaremill.sttp.client3" %% "akka-http-backend"                % Version.sttp cross CrossVersion.for3Use2_13
  final val sttpBEMonix             = "com.softwaremill.sttp.client3" %% "async-http-client-backend-monix"  % Version.sttp cross CrossVersion.for3Use2_13
  final val xstream                 = "com.thoughtworks.xstream"       % "xstream"                          % Version.xstream
  final val xml                     = "org.scala-lang.modules"        %% "scala-xml"                        % Version.xml
  final val zio                     = "dev.zio"                       %% "zio"                              % Version.zio
  final val zioStreams              = "dev.zio"                       %% "zio-streams"                      % Version.zio
  //"dev.zio"                    %% "zio-streams"    % zioVersionm,

  // test
  final val mockito             = "org.mockito"                 % "mockito-all"               % Version.mockito
  final val munit               = "org.scalameta"              %% "munit"                     % Version.munit
  final val munitScalaCheck     = "org.scalameta"              %% "munit-scalacheck"          % Version.munit
  final val scalatest           = "org.scalatest"              %% "scalatest"                 % Version.scalaTest
  final val selenium            = "org.seleniumhq.selenium"     % "selenium-java"             % Version.selenium
  final val seleniumPlus        = "org.scalatestplus"          %% "selenium-3-141"            % Version.seleniumPlus
  final val scalaCheck          = "org.scalacheck"             %% "scalacheck"                % Version.scalaCheck
  final val shapelessScalaCheck = "com.github.alexarchambault" %% "scalacheck-shapeless_1.15" % Version.scalaCheckShapeless cross CrossVersion.for3Use2_13
}

object Dependencies {
  import Library._
  val dependencies = Seq(
    akkaStream,
    catsCore,
    catsFree,
    chimney,
    circeCore,
    circeGeneric,
    circeGenericExtras,
    circeLiteral,
    circeParser,
    enumeratum,
    fs2Core,
    fs2IO,
    fs2ReactiveStreams,
    kittens,
    logback,
    monix,
    refined,
    scalaMeta,
    scalaParallelCollection,
    scalaReflect,
    scodec,
    shapeless,
    simulacrum,
    xstream,
    xml,
    zio
  )

  val testDependencies = Seq(
    mockito             % Test,
    scalatest           % "it,test",
    seleniumPlus        % "it",
    selenium            % "it",
    scalaCheck          % Test,
    shapelessScalaCheck % Test
  )
}
