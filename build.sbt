name := """scalacheat"""

version := "1.0"

scalaVersion := "2.11.6"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "2.2.4" % "test",
	"io.reactivex" %% "rxscala" % "0.24.1",
	"org.scala-lang.modules" %% "scala-pickling" % "0.10.0"
	)

// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"

