import sbt._

object Version {
  final val akkaVersion    = "2.5.26"
  final val catsVersion    = "2.0.0"
  final val circeVersion   = "0.12.3"
  final val fs2            = "2.1.0"
  final val kittensVersion = "2.0.0"
  final val monixVersion   = "3.0.0"
  final val monocleVersion = "2.0.0"
  final val scala          = "2.13.1"
  final val zioVersion     = "1.0.0-RC16"
}

object Library {
  final val akkaStream   = "com.typesafe.akka"          %% "akka-stream"    % Version.akkaVersion
  final val scalazCore   = "org.scalaz"                 %% "scalaz-core"    % "7.2.29"
  final val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2"
  final val logback      = "ch.qos.logback"             % "logback-classic" % "1.2.3"
  final val scalaReflect = "org.scala-lang"             % "scala-reflect"   % Version.scala
  final val scodec       = "org.scodec"                 %% "scodec-core"    % "1.11.4"
  final val shapeless    = "com.chuusai"                %% "shapeless"      % "2.3.3"
  final val circeCore    = "io.circe"                   %% "circe-core"     % Version.circeVersion
  final val circeGeneric = "io.circe"                   %% "circe-generic"  % Version.circeVersion
  final val circeParser  = "io.circe"                   %% "circe-parser"   % Version.circeVersion
  final val zio          = "dev.zio"                    %% "zio"            % Version.zioVersion
  final val catsConsole  = "dev.profunktor"             %% "console4cats"   % "0.8.0"
  final val pureConfig   = "com.github.pureconfig"      %% "pureconfig"     % "0.12.1"
//"dev.zio"                    %% "zio-streams"    % zioVersionm,
// fs2
  final val fs2Core            = "co.fs2" %% "fs2-core"             % Version.fs2
  final val fs2IO              = "co.fs2" %% "fs2-io"               % Version.fs2 // optional I/O library
  final val fs2ReactiveStreams = "co.fs2" %% "fs2-reactive-streams" % Version.fs2 // optional reactive streams interop
  final val fs2Experimental    = "co.fs2" %% "fs2-experimental"     % Version.fs2 // optional experimental library
// cats
  final val catsCore = "org.typelevel" %% "cats-core" % Version.catsVersion
  final val catsFree = "org.typelevel" %% "cats-free" % Version.catsVersion
  final val kittens  = "org.typelevel" %% "kittens"   % Version.kittensVersion
// monocle
  final val monocleCore    = "com.github.julien-truffaut" %% "monocle-core"    % Version.monocleVersion
  final val monocleGeneric = "com.github.julien-truffaut" %% "monocle-generic" % Version.monocleVersion
  final val monocleMacro   = "com.github.julien-truffaut" %% "monocle-macro"   % Version.monocleVersion
  final val monocleState   = "com.github.julien-truffaut" %% "monocle-state"   % Version.monocleVersion
  final val monocleRefined = "com.github.julien-truffaut" %% "monocle-refined" % Version.monocleVersion
  final val monocleLaw     = "com.github.julien-truffaut" %% "monocle-law"     % Version.monocleVersion % "test"
  final val enumeratum     = "com.beachape"               %% "enumeratum"      % "1.5.13"
  final val monix          = "io.monix"                   %% "monix"           % Version.monixVersion
  final val simulacrum     = "com.github.mpilquist"       %% "simulacrum"      % "0.19.0"
// test
  final val mockito             = "org.mockito"                % "mockito-all"                % "1.10.19"  % "test"
  final val scalatest           = "org.scalatest"              %% "scalatest"                 % "3.0.8"    % "it,test"
  final val selenium            = "org.seleniumhq.selenium"    % "selenium-java"              % "3.141.59" % "test"
  final val shapelessScalaCheck = "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3"    % "test"
  final val scalaCheck          = "org.scalacheck"             %% "scalacheck"                % "1.14.2"   % "test"
}

object Dependencies {
  val dependencies = Seq(
    Library.akkaStream,
    Library.scalazCore,
    Library.scalaLogging,
    Library.logback,
    Library.scalaReflect,
    Library.scodec,
    Library.shapeless,
    Library.circeCore,
    Library.circeGeneric,
    Library.circeParser,
    Library.zio,
    Library.catsConsole,
    Library.pureConfig,
    Library.fs2Core,
    Library.fs2IO,
    Library.fs2ReactiveStreams,
    Library.fs2Experimental,
    Library.catsCore,
    Library.catsFree,
    Library.kittens,
    Library.monocleCore,
    Library.monocleGeneric,
    Library.monocleMacro,
    Library.monocleState,
    Library.monocleRefined,
    Library.monocleLaw,
    Library.enumeratum,
    Library.monix,
    Library.simulacrum
  )

  val testDependencies = Seq(
    Library.mockito,
    Library.scalatest,
    Library.selenium,
    Library.shapelessScalaCheck,
    Library.scalaCheck
  )
}
