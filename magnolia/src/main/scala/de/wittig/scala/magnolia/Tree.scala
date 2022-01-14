package de.wittig.scala.magnolia

enum Tree[+T] derives Print:
  case Branch(left: Tree[T], right: Tree[T])
  case Leaf(value: T)
