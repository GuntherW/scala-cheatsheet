name := """scalacheat"""

version := "1.0"

scalaVersion := "2.12.4"

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
  "-Ywarn-unused",
  // Warn when imports are unused
  // "-Ywarn-unused-import",
  // Warn when non-Unit expression results are unused
  "-Ywarn-value-discard"
)

val monocleVersion = "1.4.0"
val circeVersion = "0.8.0"
val akkaVersion = "2.5.6"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "io.reactivex" %% "rxscala" % "0.26.5",
  "org.scalaz" %% "scalaz-core" % "7.2.16",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scala-lang" % "scala-reflect" % "2.12.4",
  "org.scodec" %% "scodec-core" % "1.10.3", //
  "com.chuusai" %% "shapeless" % "2.3.2",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.julien-truffaut" %% "monocle-core" % "1.4.0",
  // cats
  "org.typelevel" %% "cats" % "0.9.0",
  //monocle
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-generic" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-state" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-refined" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-law" % monocleVersion % "test",
  "com.beachape" %% "enumeratum" % "1.5.12",
  // Ammonite
  "com.lihaoyi" % "ammonite" % "1.0.3" % "test" cross CrossVersion.full,
  "com.lihaoyi" %% "ammonite-ops" % "1.0.3" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % "3.6.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"
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
addCompilerPlugin(
  "org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full)
