package de.codecentric.wittig.scala.catsRelated

import cats.implicits._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
object Traverse extends App {

  futureOptionSeq()

  def listOptions(): Unit = {
    val list1 = List(Some(2), Some(1))
    val list2 = Option(List(2, 1))

    println("---traverse---")
    println(list1.traverse(_.map(_ + 1)))
    println(list2.traverse(_.map(_ + 1)))

    println("---sequence---")
    println(list2.sequence)
  }

  def futureOptions(): Unit = {
    val fu2 = Option(Future.successful(2))

    println("---traverse---")
    println(fu2.traverse(_.map(_ + 1)))

    println("---sequence---")
    println(fu2.sequence)
  }

  def futureOptionSeq(): Unit = {
    val fu2 = Option(Future.successful(Seq(1, 2, 3)))

    println("---traverse---")
//    println(list1.traverse(_.map(_ + 1)))
    println(fu2.traverse(_.map(_.map(_ + 1))))

    println("---sequence---")
    println(fu2.sequence)
  }
}
