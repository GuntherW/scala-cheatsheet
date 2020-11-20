package de.codecentric.wittig.scala.typelevel

import scala.reflect.runtime.universe._

/**
  * https://www.youtube.com/watch?v=qwUYqv6lKtQ&list=PLmtsMNDRU0ByOQoz6lnihh6CtMrErNax7
  */
object TypelevelProgramming extends App {

  // boilerplate
  def show[T](value: T)(implicit tag: TypeTag[T]) =
    tag.toString().replace("de.codecentric.wittig.scala.typelevel.TypelevelProgramming.", "")
  println(show(List(1, 2, 3)))

  // type-level programming
  trait Nat
  class _0             extends Nat
  class Succ[N <: Nat] extends Nat

  type _1 = Succ[_0]
  type _2 = Succ[_1]
  type _3 = Succ[_2]
  type _4 = Succ[_3]
  type _5 = Succ[_4]

  // How to define relationship like _2 < _4?
  trait <[A <: Nat, B <: Nat]
  object < {
    implicit def ltBasic[B <: Nat]: _0 < Succ[B]                                        = new <[_0, Succ[B]] {}
    implicit def inductive[A <: Nat, B <: Nat](implicit lt: <[A, B]): Succ[A] < Succ[B] = new <[Succ[A], Succ[B]] {}
    def apply[A <: Nat, B <: Nat](implicit lt: <[A, B]): A < B                          = lt
  }
  val comparison1: _0 < _1 = <[_0, _1]
  val comparison2: _1 < _3 = <[_1, _3]
  println(show(comparison1))
  println(show(comparison2))

  // How to define relationship like _2 <= _2?
  trait <=[A <: Nat, B <: Nat]
  object <= {
    implicit def lteBasic[B <: Nat]: _0 <= B                                               = new <=[_0, B] {}
    implicit def inductive[A <: Nat, B <: Nat](implicit lte: <=[A, B]): Succ[A] <= Succ[B] = new <=[Succ[A], Succ[B]] {}
    def apply[A <: Nat, B <: Nat](implicit lte: <=[A, B]): A <= B                          = lte
  }
  val comparison3: _0 <= _1 = <=[_0, _1]
  val comparison4: _1 <= _1 = <=[_1, _1]
  println(show(comparison3))
  println(show(comparison4))

  // How to add numbers as types
  trait +[A <: Nat, B <: Nat] { type Result <: Nat }

  object + {
    type Plus[A <: Nat, B <: Nat, S <: Nat] = +[A, B] { type Result = S } // helper Type
    // 0+0=0
    implicit val zero: Plus[_0, _0, _0]                                                                                       = new +[_0, _0] { type Result = _0 }
    // 0+a=a
    implicit def basicRight[A <: Nat](implicit lt: _0 < A): Plus[_0, A, A]                                                    = new +[_0, A] { type Result = A }
    // a+0=a
    implicit def basicLeft[A <: Nat](implicit lt: _0 < A): Plus[A, _0, A]                                                     = new +[A, _0] { type Result = A }
    // a+b = Succ[a]+Succ[b] = Succ[Succ[s]]
    implicit def inductive[A <: Nat, B <: Nat, S <: Nat](implicit plus: Plus[A, B, S]): Plus[Succ[A], Succ[B], Succ[Succ[S]]] = new +[Succ[A], Succ[B]] { type Result = Succ[Succ[S]] }

    def apply[A <: Nat, B <: Nat](implicit plus: +[A, B]): Plus[A, B, plus.Result] = plus
  }
  val zero: +[_0, _0] = +.apply
  val four: +[_1, _3] = +.apply
  println(show(four))            // TypeTag[_1 + _3]
  println(show(+.apply[_1, _3])) // TypeTag[Succ[_0] + Succ[Succ[Succ[_0]]]{type Result = Succ[Succ[Succ[Succ[_0]]]]}]

  // How to sort a List
  trait HList
  class HNil                     extends HList
  class ::[H <: Nat, T <: HList] extends HList

  // 1. split list in half
  // 2. sort the halves
  // 3. merge them back

  trait Split[Hl <: HList, L <: HList, R <: HList]
  object Split {
    implicit val basic: Split[HNil, HNil, HNil]                                                                                                              = new Split[HNil, HNil, HNil] {}
    implicit def basic2[N <: Nat]: Split[N :: HNil, N :: HNil, HNil]                                                                                         = new Split[N :: HNil, N :: HNil, HNil] {}
    implicit def inductive[N1 <: Nat, N2 <: Nat, T <: HList, L <: HList, R <: HList](implicit split: Split[T, L, R]): Split[N1 :: N2 :: T, N1 :: L, N2 :: R] =
      new Split[N1 :: N2 :: T, N1 :: L, N2 :: R] {}
  }

}
