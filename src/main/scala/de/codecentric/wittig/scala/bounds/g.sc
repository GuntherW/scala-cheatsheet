package de.codecentric.wittig.scala.bounds

object g {

  import scala.language.higherKinds

  trait Container[M[_]] {
    def put[A](x: A): M[A]
    def get[A](m: M[A]): A
  }

  val container = new Container[List] {
    def put[A](x: A)       = List(x)
    def get[A](m: List[A]) = m.head
  } //> container  : de.codecentric.wittig.scala.bounds.g.Container[List] = de.codec
  //| entric.wittig.scala.bounds.g$$anonfun$main$1$$anon$1@6aadfaec

  container.put("123")             //> res0: List[String] = List(123)
  container.put(123)               //> res1: List[Int] = List(123)
  container.get(List(Some("123"))) //> res2: Some[String] = Some(123)

  implicit val listContainer = new Container[List] {
    def put[A](x: A)       = List(x)
    def get[A](m: List[A]) = m.head
  } //> listContainer  : de.codecentric.wittig.scala.bounds.g.Container[List] = de.c
  //| odecentric.wittig.scala.bounds.g$$anonfun$main$1$$anon$2@4282c39f

  implicit val optionContainer = new Container[Some] {
    def put[A](x: A)       = Some(x)
    def get[A](m: Some[A]) = m.get
  } //> optionContainer  : de.codecentric.wittig.scala.bounds.g.Container[Some] = de
  //| .codecentric.wittig.scala.bounds.g$$anonfun$main$1$$anon$3@111c4d14

  /**
	*import scala.tools.nsc.typechecker.Adaptations
	*def tupleize[M[_]: Container, A, B](fst: M[A], snd: M[B]) = {
  *val c = implicitly[Container[M]]
  *c.put(c.get(fst), c.get(snd))
  *}
  *tupleize(Some(1), Some(2))
  *tupleize(List(1), List(2))
  */
}
