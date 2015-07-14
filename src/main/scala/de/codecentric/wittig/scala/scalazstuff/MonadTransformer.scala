package de.codecentric.wittig.scala.scalazstuff
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.OptionT
import scalaz._
import Scalaz._
/**
 * Ein Monadtransformer ist eine Monade, die das Arbeiten mit zwei ineinaner verschachtelte Monaden erleichtert.
 *
 *
 *
 * @author gunther
 */
object MonadTransformer extends App {

  val getX: Future[Option[Int]] = Future(Some(2))
  val getY: Future[Option[Int]] = Future(Some(3))

  val z: OptionT[Future, Int] = for {
    x <- OptionT(getX)
    y <- OptionT(getY)
  } yield x + y

  z.run.foreach(println) // prints Some(5)
}
