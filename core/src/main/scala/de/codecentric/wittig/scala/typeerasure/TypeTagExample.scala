package de.codecentric.wittig.scala.typeerasure

/**
  * http://squidarth.com/scala/types/2019/01/11/type-erasure-scala.html
  *
  * =:= (type equality)
  * <:< (subtype relation)
  */
object TypeTagExample extends App {
  println(processThing(Thing(1)))
  println(processThing(Thing("hello")))
  println(processThing(Thing(Seq(1))))
  println(processThing(Thing(Seq("1"))))

  println(processThingWithTypeTag(Thing(1)))
  println(processThingWithTypeTag(Thing("hello")))
  println(processThingWithTypeTag(Thing(Seq(1))))
  println(processThingWithTypeTag(Thing(Seq("1"))))

  // Example with type erasure problem
  def processThing(thing: Thing[_]): String = {
    thing match {
      case Thing(value: Int)                 => "Thing of int"
      case Thing(value: String)              => "Thing of string"
      case Thing(value: Seq[Int] @unchecked) => "Thing of Seq[Int]" // Type erasure // will produce compiler warning
      case _                                 => "Thing of something else"
    }
  }

  // a way to identify type at runtime
  import scala.reflect.runtime.universe._
  def processThingWithTypeTag[T: TypeTag](thing: Thing[T]): String = {
    typeOf[T] match {
      case t if t =:= typeOf[Seq[Int]]    => "Thing of Seq[Int]"
      case t if t =:= typeOf[Seq[String]] => "Thing of Seq[String]"
      case t if t =:= typeOf[Int]         => "Thing of Int"
      case _                              => "Thing of other"
    }
  }

  // way how to do it
  def processThingHowToDoIt[T: TypeTag](thing: Thing[T]): String = {
    thing match {
      case Thing(value: Int)                                                   => "Thing of int " + value.toString
      case Thing(value: Seq[Int] @unchecked) if typeOf[T] =:= typeOf[Seq[Int]] => "Thing of seq of int" + value.sum
      case _                                                                   => "Thing of something else"
    }
  }
}

case class Thing[T](value: T)
