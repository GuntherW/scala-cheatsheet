package de.codecentric.wittig.scala.scalacheck

import org.scalacheck._
import Gen._
import Arbitrary.arbitrary
import org.scalacheck.Prop.forAll

/**
 * @author gunther
 */
object CaseClasses extends Properties("Tree") {

  //Mein Model
  sealed abstract class Tree
  case class Node(left: Tree, right: Tree, v: Int) extends Tree
  case object Leaf extends Tree

  val genLeaf = const(Leaf)

  val genNode = for {
    v <- arbitrary[Int]
    left <- genTree
    right <- genTree
  } yield Node(left, right, v)

  def genTree: Gen[Tree] = oneOf(genLeaf, genNode)

  property("sample Tree") = forAll(genTree) { (a: Tree) =>
    a == a
    //    println(a.sample)
  }
  property("substring") = forAll { (a: String, b: String, c: String) =>
    (a + b + c).substring(a.length, a.length + b.length) == b
  }
}
