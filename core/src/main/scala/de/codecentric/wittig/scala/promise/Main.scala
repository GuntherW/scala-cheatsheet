package de.codecentric.wittig.scala.promise
import de.codecentric.wittig.scala.Ops.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/** Where Future provides an interface exclusively for querying, Promise is a companion type that allows you to complete a Future by putting a value into it. This can be done exactly once.
  */
@main
def main(): Unit =
  def foo: Future[Foo] =
    val p = Promise[Foo]()
    Future {
      Thread.sleep(2000)
      p.success(Foo("hallo")) // Promise may not be completed in caller thread, but in some other "runnable"
//      p.failure(new Exception("Fehler"))
    }
    p.future
  println(foo.await)

case class Foo(name: String)
