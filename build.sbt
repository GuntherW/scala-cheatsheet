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
	"io.reactivex" %% "rxscala" % "0.26.0",
	"org.scala-lang.modules" %% "scala-pickling" % "0.10.2-SNAPSHOT",
	
	"org.scalaz" %% "scalaz-core" % "7.2.1",
	
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"ch.qos.logback" % "logback-classic" % "1.1.6",
	
	"org.scala-lang" % "scala-reflect" % "2.11.7",
	
	"org.scodec" % "scodec-core_2.11" % "1.9.0", // 
	
	"com.github.mpilquist" %% "simulacrum" % "0.7.0", // Functor
	"org.scalamacros" % "paradise_2.11.6" % "2.1.0", // @typeclass Functor
	
	"com.chuusai" %% "shapeless" % "2.3.0",
	
	"io.circe" %% "circe-core" % "0.3.0",
  	"io.circe" %% "circe-generic" % "0.3.0",
  	"io.circe" %% "circe-parser" % "0.3.0",

	// cats  	
	"org.typelevel" %% "cats" % "0.4.1",
	
		// Ammonite
    "com.lihaoyi" % "ammonite-repl" % "0.5.5" cross CrossVersion.full,
		
	"org.mockito" % "mockito-all" % "1.10.19" % "test",
	"org.scalatest" %% "scalatest" % "2.2.6" % "test",
	"org.seleniumhq.selenium" % "selenium-java" % "2.52.0" % "test",
	"org.scalacheck" %% "scalacheck" % "1.13.0" % "test"	
	)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("pulblic")
resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

// Improved Incremental compilation
//incOptions := incOptions.value.withNameHashing(true)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.6.0" cross CrossVersion.binary)// Improved dependency management
	
updateOptions := updateOptions.value.withCachedResolution(true)

EclipseKeys.preTasks := Seq(compile in Compile)                  // Compile the project before generating Eclipse files, so that .class files for views and routes are present

tutSettings




/// Ammonite
// Ammonite
val ammoniteInitialCommands = """
  |import scala.concurrent._
  |import scala.concurrent.duration._
  |import scala.concurrent.ExecutionContext.Implicits.global
  |repl.prompt() = "@> "
  |repl.colors() = ammonite.repl.Colors.Default.copy(
  |  prompt  = ammonite.repl.Ref(Console.BLUE),
  |  `type`  = ammonite.repl.Ref(Console.CYAN),
  |  literal = ammonite.repl.Ref(Console.YELLOW),
  |  comment = ammonite.repl.Ref(Console.WHITE),
  |  keyword = ammonite.repl.Ref(Console.RED)
  |)
""".trim.stripMargin

initialCommands in console := s"""
  |ammonite.repl.Main.run(\"\"\"$ammoniteInitialCommands\"\"\")
""".trim.stripMargin
