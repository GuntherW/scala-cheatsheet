import sbt._

object Version {
  final val akkaVersion             = "2.6.14"
  final val catsVersion             = "2.6.1"
  final val catsConsole             = "0.8.1"
  final val chimney                 = "0.6.1"
  final val circeVersion            = "0.13.0"
  final val enumeratum              = "1.6.1"
  final val fs2                     = "2.5.6"
  final val kittensVersion          = "2.3.1"
  final val logback                 = "1.2.3"
  final val magnolia                = "0.17.0"
  final val monix                   = "3.4.0"
  final val monocleVersion          = "2.1.0"
  final val pureConfig              = "0.15.0"
  final val reflect                 = scala
  final val refined                 = "0.9.25"
  final val scala                   = "2.13.5"
//  final val scala                   = "3.0.0"
  final val scalaMeta               = "4.4.17"
  final val scalaParallelCollection = "1.0.3"
  final val scalaz                  = "7.2.29"
  final val scodec                  = "1.11.7"
  final val simulacrum              = "0.19.0"
  final val shapeless               = "2.3.7"
  final val sttp                    = "3.3.3"
  final val xstream                 = "1.4.17"
  final val xml                     = "2.0.0-M5"
  final val zio                     = "1.0.7"
  final val zioKafka                = "0.14.0"
  // Testre
  final val mockito                 = "1.10.19"
  final val munit                   = "0.7.26"
  final val scalaTest               = "3.2.9"
  final val selenium                = "3.141.59"
  final val seleniumPlus            = "3.2.9.0"
  final val scalaCheck              = "1.15.4"
  final val scalaCheckShapeless     = "1.2.5"
}

object Library {
  final val akkaStream              = "com.typesafe.akka"             %% "akka-stream"                      % Version.akkaVersion
  final val catsCore                = "org.typelevel"                 %% "cats-core"                        % Version.catsVersion
  final val catsFree                = "org.typelevel"                 %% "cats-free"                        % Version.catsVersion
  final val chimney                 = "io.scalaland"                  %% "chimney"                          % Version.chimney
  final val circeCore               = "io.circe"                      %% "circe-core"                       % Version.circeVersion
  final val circeGeneric            = "io.circe"                      %% "circe-generic"                    % Version.circeVersion
  final val circeGenericExtras      = "io.circe"                      %% "circe-generic-extras"             % Version.circeVersion
  final val circeParser             = "io.circe"                      %% "circe-parser"                     % Version.circeVersion
  final val circeLiteral            = "io.circe"                      %% "circe-literal"                    % Version.circeVersion
  final val enumeratum              = "com.beachape"                  %% "enumeratum"                       % Version.enumeratum
  final val fs2Core                 = "co.fs2"                        %% "fs2-core"                         % Version.fs2
  final val fs2IO                   = "co.fs2"                        %% "fs2-io"                           % Version.fs2 // optional I/O library
  final val fs2ReactiveStreams      = "co.fs2"                        %% "fs2-reactive-streams"             % Version.fs2 // optional reactive streams interop
  final val kittens                 = "org.typelevel"                 %% "kittens"                          % Version.kittensVersion
  final val logback                 = "ch.qos.logback"                 % "logback-classic"                  % Version.logback
  final val magnolia                = "com.propensive"                %% "magnolia"                         % Version.magnolia
  final val monocleCore             = "com.github.julien-truffaut"    %% "monocle-core"                     % Version.monocleVersion
  final val monocleGeneric          = "com.github.julien-truffaut"    %% "monocle-generic"                  % Version.monocleVersion
  final val monix                   = "io.monix"                      %% "monix"                            % Version.monix
  final val monocleMacro            = "com.github.julien-truffaut"    %% "monocle-macro"                    % Version.monocleVersion
  final val monocleRefined          = "com.github.julien-truffaut"    %% "monocle-refined"                  % Version.monocleVersion
  final val monocleState            = "com.github.julien-truffaut"    %% "monocle-state"                    % Version.monocleVersion
  final val pureConfig              = "com.github.pureconfig"         %% "pureconfig"                       % Version.pureConfig
  final val refined                 = "eu.timepit"                    %% "refined"                          % Version.refined
  final val reflect                 = "org.scala-lang"                 % "scala-reflect"                    % Version.reflect
  final val scalaMeta               = "org.scalameta"                 %% "scalameta"                        % Version.scalaMeta
  final val scalaParallelCollection = "org.scala-lang.modules"        %% "scala-parallel-collections"       % Version.scalaParallelCollection
  final val scalaReflect            = "org.scala-lang"                 % "scala-reflect"                    % Version.scala
  final val shapeless               = "com.chuusai"                   %% "shapeless"                        % Version.shapeless
  final val simulacrum              = "com.github.mpilquist"          %% "simulacrum"                       % Version.simulacrum
  final val scodec                  = "org.scodec"                    %% "scodec-core"                      % Version.scodec
  final val sttpCore                = "com.softwaremill.sttp.client3" %% "core"                             % Version.sttp
  final val sttpCirce               = "com.softwaremill.sttp.client3" %% "circe"                            % Version.sttp
  final val sttpBEAsync             = "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % Version.sttp
  final val sttpBEZio               = "com.softwaremill.sttp.client3" %% "httpclient-backend-zio"           % Version.sttp
  final val sttpBEAkkaHttp          = "com.softwaremill.sttp.client3" %% "akka-http-backend"                % Version.sttp
  final val sttpBEMonix             = "com.softwaremill.sttp.client3" %% "async-http-client-backend-monix"  % Version.sttp

  final val xstream = "com.thoughtworks.xstream" % "xstream"   % Version.xstream
  final val xml     = "org.scala-lang.modules"  %% "scala-xml" % Version.xml
  final val zio     = "dev.zio"                 %% "zio"       % Version.zio
  //"dev.zio"                    %% "zio-streams"    % zioVersionm,

  // test
  final val mockito             = "org.mockito"                 % "mockito-all"               % Version.mockito
  final val monocleLaw          = "com.github.julien-truffaut" %% "monocle-law"               % Version.monocleVersion
  final val munit               = "org.scalameta"              %% "munit"                     % Version.munit
  final val munitScalaCheck     = "org.scalameta"              %% "munit-scalacheck"          % Version.munit
  final val scalatest           = "org.scalatest"              %% "scalatest"                 % Version.scalaTest
  final val selenium            = "org.seleniumhq.selenium"     % "selenium-java"             % Version.selenium
  final val seleniumPlus        = "org.scalatestplus"          %% "selenium-3-141"            % Version.seleniumPlus
  final val scalaCheck          = "org.scalacheck"             %% "scalacheck"                % Version.scalaCheck
  final val shapelessScalaCheck = "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % Version.scalaCheckShapeless
}

object Dependencies {
  val dependencies = Seq(
    Library.akkaStream,
    Library.catsCore,
    Library.catsFree,
    Library.chimney,
    Library.circeCore,
    Library.circeGeneric,
    Library.circeGenericExtras,
    Library.circeLiteral,
    Library.circeParser,
    Library.enumeratum,
    Library.fs2Core,
    Library.fs2IO,
    Library.fs2ReactiveStreams,
    Library.kittens,
    Library.logback,
    Library.monix,
    Library.monocleCore,
    Library.monocleGeneric,
    Library.monocleMacro,
    Library.monocleState,
    Library.monocleRefined,
    Library.monocleLaw,
    Library.pureConfig,
    Library.refined,
    Library.scalaMeta,
    Library.scalaParallelCollection,
    Library.scalaReflect,
    Library.scodec,
    Library.shapeless,
    Library.simulacrum,
    Library.xstream,
    Library.xml,
    Library.zio
  )

  val zioDependencies = Seq(
    Library.zio,
    "dev.zio" %% "zio-streams" % Version.zio,
    "dev.zio" %% "zio-kafka"   % Version.zioKafka
  )

  val testDependencies = Seq(
    Library.mockito             % Test,
    Library.scalatest           % "it,test",
    Library.seleniumPlus        % "it",
    Library.selenium            % "it",
    Library.scalaCheck          % Test,
    Library.shapelessScalaCheck % Test
  )
}
