name := """scalacheat"""

version := "1.0"

scalaVersion := "2.11.7"


scalacOptions ++= Seq(
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
    // Warn when deat code is identified
    "-Ywarn-dead-code",
    // Warn when local and private vals, vars, defs, and types are unused
    "-Ywarn-unused",
    // Warn when imports are unused
    // "-Ywarn-unused-import",
    // Warn when non-Unit expression results are unused
    "-Ywarn-value-discard"
)

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
	"io.reactivex" %% "rxscala" % "0.25.0",
	"org.scala-lang.modules" %% "scala-pickling" % "0.10.2-SNAPSHOT",
	
	"org.scalaz" %% "scalaz-core" % "7.1.3",
	
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"ch.qos.logback" % "logback-classic" % "1.1.3",
	
	"org.scala-lang" % "scala-reflect" % "2.11.7",
	
	"org.scodec" % "scodec-core_2.11" % "1.8.1", // 
	
	"com.github.mpilquist" % "simulacrum_2.11" % "0.4.0", // Functor
	"org.scalamacros" % "paradise_2.11.6" % "2.1.0-M5", // @typeclass Functor
	
	"org.mockito" % "mockito-all" % "1.10.19" % "test",
	"org.scalatest" %% "scalatest" % "2.2.5" % "test",
	"org.seleniumhq.selenium" % "selenium-java" % "2.47.1" % "test",
	"org.scalacheck" %% "scalacheck" % "1.12.3" % "test"
	
	)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("pulblic")
resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

// Improved Incremental compilation
//incOptions := incOptions.value.withNameHashing(true)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)

// addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.6.0" cross CrossVersion.binary)// Improved dependency management
	
updateOptions := updateOptions.value.withCachedResolution(true)

EclipseKeys.preTasks := Seq(compile in Compile)                  // Compile the project before generating Eclipse files, so that .class files for views and routes are present

