import sbt._

object Version {
  final val akkaVersion    = "2.6.3"
  final val catsVersion    = "2.1.0"
  final val catsConsole    = "0.8.1"
  final val chimney        = "0.4.1"
  final val circeVersion   = "0.13.0"
  final val enumeratum     = "1.5.15"
  final val fs2            = "2.2.2"
  final val kittensVersion = "2.0.0"
  final val logback        = "1.2.3"
  final val monix          = "3.1.0"
  final val monocleVersion = "2.0.1"
  final val pureConfig     = "0.12.2"
  final val scala          = "2.13.1"
  final val scalaLogging   = "3.9.2"
  final val scalaz         = "7.2.29"
  final val scodec         = "1.11.4"
  final val simulacrum     = "0.19.0"
  final val shapeless      = "2.3.3"
  final val zioVersion     = "1.0.0-RC17"

  // Test
  final val mockito             = "1.10.19"
  final val scalaTest           = "3.1.1"
  final val selenium            = "3.141.59"
  final val scalaCheck          = "1.14.3"
  final val shapelessScalaCheck = "1.2.4"
}

object Library {
  final val akkaStream         = "com.typesafe.akka"          %% "akka-stream"          % Version.akkaVersion
  final val catsConsole        = "dev.profunktor"             %% "console4cats"         % Version.catsConsole
  final val catsCore           = "org.typelevel"              %% "cats-core"            % Version.catsVersion
  final val catsFree           = "org.typelevel"              %% "cats-free"            % Version.catsVersion
  final val chimney            = "io.scalaland"               %% "chimney"              % Version.chimney
  final val circeCore          = "io.circe"                   %% "circe-core"           % Version.circeVersion
  final val circeGeneric       = "io.circe"                   %% "circe-generic"        % Version.circeVersion
  final val circeParser        = "io.circe"                   %% "circe-parser"         % Version.circeVersion
  final val enumeratum         = "com.beachape"               %% "enumeratum"           % Version.enumeratum
  final val fs2Core            = "co.fs2"                     %% "fs2-core"             % Version.fs2
  final val fs2IO              = "co.fs2"                     %% "fs2-io"               % Version.fs2 // optional I/O library
  final val fs2Experimental    = "co.fs2"                     %% "fs2-experimental"     % Version.fs2 // optional experimental library
  final val fs2ReactiveStreams = "co.fs2"                     %% "fs2-reactive-streams" % Version.fs2 // optional reactive streams interop
  final val kittens            = "org.typelevel"              %% "kittens"              % Version.kittensVersion
  final val logback            = "ch.qos.logback"             % "logback-classic"       % Version.logback
  final val monocleCore        = "com.github.julien-truffaut" %% "monocle-core"         % Version.monocleVersion
  final val monocleGeneric     = "com.github.julien-truffaut" %% "monocle-generic"      % Version.monocleVersion
  final val monix              = "io.monix"                   %% "monix"                % Version.monix
  final val monocleMacro       = "com.github.julien-truffaut" %% "monocle-macro"        % Version.monocleVersion
  final val monocleRefined     = "com.github.julien-truffaut" %% "monocle-refined"      % Version.monocleVersion
  final val monocleState       = "com.github.julien-truffaut" %% "monocle-state"        % Version.monocleVersion
  final val pureConfig         = "com.github.pureconfig"      %% "pureconfig"           % Version.pureConfig
  final val scalaLogging       = "com.typesafe.scala-logging" %% "scala-logging"        % Version.scalaLogging
  final val scalaReflect       = "org.scala-lang"             % "scala-reflect"         % Version.scala
  final val shapeless          = "com.chuusai"                %% "shapeless"            % Version.shapeless
  final val simulacrum         = "com.github.mpilquist"       %% "simulacrum"           % Version.simulacrum
  final val scodec             = "org.scodec"                 %% "scodec-core"          % Version.scodec
  final val zio                = "dev.zio"                    %% "zio"                  % Version.zioVersion
  //"dev.zio"                    %% "zio-streams"    % zioVersionm,

  // test
  final val mockito             = "org.mockito"                % "mockito-all"                % Version.mockito             % Test
  final val monocleLaw          = "com.github.julien-truffaut" %% "monocle-law"               % Version.monocleVersion      % Test
  final val scalatest           = "org.scalatest"              %% "scalatest"                 % Version.scalaTest           % "it,test"
  final val selenium            = "org.seleniumhq.selenium"    % "selenium-java"              % Version.selenium            % Test
  final val scalaCheck          = "org.scalacheck"             %% "scalacheck"                % Version.scalaCheck          % Test
  final val shapelessScalaCheck = "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % Version.shapelessScalaCheck % Test
}

object Dependencies {
  val dependencies = Seq(
    Library.akkaStream,
    Library.catsConsole,
    Library.catsCore,
    Library.catsFree,
    Library.chimney,
    Library.circeCore,
    Library.circeGeneric,
    Library.circeParser,
    Library.enumeratum,
    Library.fs2Core,
    Library.fs2IO,
    Library.fs2ReactiveStreams,
    Library.fs2Experimental,
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
    Library.scalaLogging,
    Library.scalaReflect,
    Library.scodec,
    Library.shapeless,
    Library.simulacrum,
    Library.zio
  )

  val testDependencies = Seq(
    Library.mockito,
    Library.scalatest,
    Library.selenium,
    Library.scalaCheck,
    Library.shapelessScalaCheck
  )
}
