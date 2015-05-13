name := """scalacheat"""

version := "1.0"

scalaVersion := "2.11.6"


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
    "-Ywarn-unused-import",
    // Warn when non-Unit expression results are unused
    "-Ywarn-value-discard"
)

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "2.2.4" % "test",
	"io.reactivex" %% "rxscala" % "0.24.1",
	"org.scala-lang.modules" %% "scala-pickling" % "0.10.0"
	)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

// Improved Incremental compilation
incOptions := incOptions.value.withNameHashing(true)

// Improved dependency management
updateOptions := updateOptions.value.withCachedResolution(true)