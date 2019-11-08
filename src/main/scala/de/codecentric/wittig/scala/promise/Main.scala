package de.codecentric.wittig.scala.promise
import de.codecentric.wittig.scala.Implicits.RichFuture

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  *  Where Future provides an interface exclusively for querying, Promise is a companion type that allows you to complete a Future by putting a value into it.
  *  This can be done exactly once.
  */
object Main extends App {
  def getFoo(): Future[Foo] = {
    val p = Promise[Foo]
    Future {
      Thread.sleep(2000)
      p.success(Foo("hallo")) // Promise may not be completed in caller thread, but in some other "runnable"
//      p.failure(new Exception("Fehler"))
    }
    p.future
  }

  println(getFoo().await)
}

case class Foo(name: String)
