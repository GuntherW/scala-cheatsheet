name := """scalacheat"""

version := "1.0"

scalaVersion := "2.12.8"

organization := "de.wittig"

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
  "-Yno-adapted-args",
  // Warn when dead code is identified
  "-Ywarn-dead-code",
  // Warn when local and private vals, vars, defs, and types are unused
//  "-Ywarn-unused",
  // Warn when imports are unused
  // "-Ywarn-unused-import",
  // Warn when non-Unit expression results are unused
  "-Ywarn-value-discard",

  "-Ypartial-unification"
)

val monocleVersion = "1.5.0"
val circeVersion = "0.11.1"
val akkaVersion = "2.5.21"
val catsVersion = "1.6.0"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "io.reactivex" %% "rxscala" % "0.26.5",
  "org.scalaz" %% "scalaz-core" % "7.2.27",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scala-lang" % "scala-reflect" % "2.12.8",
  "org.scodec" %% "scodec-core" % "1.11.3", //
  "com.chuusai" %% "shapeless" % "2.3.3",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.julien-truffaut" %% "monocle-core" % "1.5.0",
  "org.scalaz" %% "scalaz-zio" % "0.12", // ZIO

  // cats
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-free" % catsVersion,
  //monocle
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-generic" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-state" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-refined" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % "test",
  "com.beachape" %% "enumeratum" % "1.5.13",
  "io.monix" %% "monix" % "3.0.0-RC2",
  "com.github.mpilquist" %% "simulacrum" % "0.15.0",


// Ammonite
 // "com.lihaoyi" % "ammonite" % "1.1.0" % "test" cross CrossVersion.full,
 // "com.lihaoyi" %% "ammonite-ops" % "1.1.0" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "3.0.6" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % "3.141.59" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("pulblic")
resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

// Improved Incremental compilation
//incOptions := incOptions.value.withNameHashing(true)

// addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.6.0" cross CrossVersion.binary)// Improved dependency management

updateOptions := updateOptions.value.withCachedResolution(true)

//EclipseKeys.preTasks := Seq(compile in Compile) // Compile the project before generating Eclipse files, so that .class files for views and routes are present
//EclipseKeys.withBundledScalaContainers := false

//tutSettings

initialCommands in (Test, console) := """ammonite.Main().run()"""

// for @Lenses macro support
addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full)


updateConfiguration in updateSbtClassifiers := (updateConfiguration in updateSbtClassifiers).value.withMissingOk(true)
