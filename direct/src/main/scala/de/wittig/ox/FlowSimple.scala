package de.wittig.ox

import scala.concurrent.duration.DurationInt

import ox.{sleep, supervised, tap}
import ox.flow.Flow
import sttp.tapir.server.netty.sync.NettySyncServer

object FlowSimple extends App {

//  flowSimple
//  demoZip
//  demoMerge
//  demoSupervised
  demoMapPar

  def flowSimple = Flow.fromValues(10, 3, 5, 12)
    .map(_ + 1)
//    .filter(_ % 2 == 0)
    .intersperse(1)
    .mapStateful(0) { (state, value) =>
      val newState = state + value
      (newState, newState)
    }
    .runToList()
    .tap(println)

  def demoZip = {
    val loopingLetters = Flow.repeat('a' to 'z').mapConcat(identity)
    val numbers        = Flow.iterate(0)(_ + 1)
    loopingLetters.zip(numbers).take(30).runForeach(println)
  }

  def demoMerge = {
    val f1 = Flow.tick(150.millis, "links")
    val f2 = Flow.tick(300.millis, "rechts")
    val f3 = Flow.tick(500.millis, 123)
    f1.merge(f2).merge(f3)
      .take(10)
      .runForeach(println)
  }

  inline def namesFlow = Flow
    .fromInputStream(this.getClass.getResourceAsStream("/names.txt"))
    .linesUtf8
    .map(_.toLowerCase.capitalize)

  inline def demoMapPar = {
    import sttp.client4.quick.*
    println("start")
    namesFlow
      .mapPar(4) { name =>
        println(quickRequest.get(uri"http://localhost:8080/hello?name=$name").send().body)
      }
      .runDrain()
  }

  // create channels & transform them using high-level operations
  inline def demoSupervised = supervised {
    Flow.iterate(0)(_ + 1) // natural numbers
      .filter(_ % 2 == 0)
      .map(_ + 1)
      .take(10)
      .runForeach(n => println(n.toString))
  }
}

@main def startHttpServer(): Unit =
  import scala.util.Random

  import sttp.tapir.*

  val helloWorld = endpoint.get
    .in("hello")
    .in(query[String]("name"))
    .out(stringBody)
    .handleSuccess { name =>
      sleep(Random.nextInt(1000).millis)
      s"Hello, $name"
    }

  NettySyncServer().addEndpoint(helloWorld).startAndWait()
