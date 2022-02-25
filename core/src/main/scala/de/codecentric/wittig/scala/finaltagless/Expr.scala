package de.codecentric.wittig.scala.finaltagless

trait Expr[A]:
  def add(a: A, b: A): A
  def zero: A
  def one: A
