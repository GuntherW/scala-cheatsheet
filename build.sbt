name := """scalacheat"""

version := "1.0"

scalaVersion := "2.11.8"

val monocleVersion = "1.3.1"

scalacOptions ++= Seq(
	"-language:_",
	"-target:jvm-1.8",
	"-encoding", "UTF-8",
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




// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
	"io.reactivex" %% "rxscala" % "0.26.3",
	"org.scala-lang.modules" %% "scala-pickling" % "0.11.0-M2",
	
	"org.scalaz" %% "scalaz-core" % "7.2.6",
	
	"com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
	"ch.qos.logback" % "logback-classic" % "1.1.7",
	
	"org.scala-lang" % "scala-reflect" % "2.11.8",
	
	"org.scodec" %% "scodec-core" % "1.10.3", // 
	
	"com.github.mpilquist" %% "simulacrum" % "0.10.0", // Functor
	"org.scalamacros" % "paradise_2.11.6" % "2.1.0", // @typeclass Functor
	
	"com.chuusai" %% "shapeless" % "2.3.2",
	
	"io.circe" %% "circe-core" % "0.5.4",
  	"io.circe" %% "circe-generic" % "0.5.4",
  	"io.circe" %% "circe-parser" % "0.5.4",

	// cats  	
	"org.typelevel" %% "cats" % "0.7.2",
	
	
	//monocle
  "com.github.julien-truffaut"  %%  "monocle-core"    % monocleVersion,
  "com.github.julien-truffaut"  %%  "monocle-generic" % monocleVersion,
  "com.github.julien-truffaut"  %%  "monocle-macro"   % monocleVersion,        
  "com.github.julien-truffaut"  %%  "monocle-state"   % monocleVersion,     
  "com.github.julien-truffaut"  %%  "monocle-refined" % monocleVersion,
  "com.github.julien-truffaut"  %%  "monocle-law"     % monocleVersion % "test",
	
	
	
		// Ammonite
    "com.lihaoyi" % "ammonite-repl" % "0.5.7" cross CrossVersion.full,
		
	"org.mockito" % "mockito-all" % "1.10.19" % "test",
	"org.scalatest" %% "scalatest" % "3.0.0" % "test",
	"org.seleniumhq.selenium" % "selenium-java" % "3.0.1" % "test",
	"org.scalacheck" %% "scalacheck" % "1.13.3" % "test"	
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


// for @Lenses macro support
addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)