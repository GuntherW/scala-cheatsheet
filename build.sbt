name := """scala-cheatsheet"""

version := "1.0"

scalaVersion := scala

organization := "de.wittig"
lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings
  )
scalacOptions ++= Seq(
  "-language:_",
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  // Emit warning for usages of features that should be impoirted explicitly
  "-feature",
  // Emit warning for usages of deprecated APIs
  "-deprecation",
  // Enable additional warnings where generated code depends on assumptions
  "-unchecked",
  // Fail the compilation if there are any warnings
  //"-Xfatal-warnings",
  // Enable or disable specific warnings
  "-Xlint:_",
  //Do not adapt an argument list to match the receiver -> z.B. List(1,2,3).toSet()
  "-Ywarn-dead-code",
  // Warn when local and private vals, vars, defs, and types are unused
//  "-Ywarn-unused",
  // Warn when imports are unused
  // "-Ywarn-unused-import",
  // Warn when non-Unit expression results are unused
  "-Ywarn-value-discard",
  "-Ymacro-annotations" // scala 2.13.0
)

val akkaVersion    = "2.5.26"
val catsVersion    = "2.0.0"
val circeVersion   = "0.12.3"
val fs2            = "2.0.1"
val kittensVersion = "2.0.0"
val monixVersion   = "3.0.0"
val monocleVersion = "2.0.0"
val scala          = "2.13.1"
val zioVersion     = "1.0.0-RC15"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-stream"    % akkaVersion,
  "org.scalaz"                 %% "scalaz-core"    % "7.2.29",
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2",
  "ch.qos.logback"             % "logback-classic" % "1.2.3",
  "org.scala-lang"             % "scala-reflect"   % scala,
  "org.scodec"                 %% "scodec-core"    % "1.11.4", //
  "com.chuusai"                %% "shapeless"      % "2.3.3",
  "io.circe"                   %% "circe-core"     % circeVersion,
  "io.circe"                   %% "circe-generic"  % circeVersion,
  "io.circe"                   %% "circe-parser"   % circeVersion,
  "dev.zio"                    %% "zio"            % zioVersion,
  "dev.profunktor"             %% "console4cats"   % "0.8.0",
  //"dev.zio"                    %% "zio-streams"    % zioVersionm,
  // fs2
  "co.fs2" %% "fs2-core"             % fs2,
  "co.fs2" %% "fs2-io"               % fs2, // optional I/O library
  "co.fs2" %% "fs2-reactive-streams" % fs2, // optional reactive streams interop
  "co.fs2" %% "fs2-experimental"     % fs2, // optional experimental library
  // cats
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-free" % catsVersion,
  "org.typelevel" %% "kittens"   % kittensVersion,
  //monocle
  "com.github.julien-truffaut" %% "monocle-core"    % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-generic" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro"   % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-state"   % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-refined" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-law"     % monocleVersion % "test",
  "com.beachape"               %% "enumeratum"      % "1.5.13",
  "io.monix"                   %% "monix"           % monixVersion,
  "com.github.mpilquist"       %% "simulacrum"      % "0.19.0",
  "org.mockito"                % "mockito-all"      % "1.10.19" % "test",
  "org.scalatest"              %% "scalatest"       % "3.0.8" % "it,test",
  "org.seleniumhq.selenium"    % "selenium-java"    % "3.141.59" % "test",
  "org.scalacheck"             %% "scalacheck"      % "1.14.2" % "test"
)

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("pulblic")
resolvers += "bintray/non" at "https://dl.bintray.com/non/maven"

updateOptions := updateOptions.value.withCachedResolution(true)

updateConfiguration in updateSbtClassifiers := (updateConfiguration in updateSbtClassifiers).value.withMissingOk(true)

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  Resolver.bintrayRepo("hseeberger", "maven")
)

turbo := true

scalafmtOnCompile := true
