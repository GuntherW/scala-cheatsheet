package de.codecentric.wittig.scala.treiberstack

trait Stack[T]:
  def push(value: T): Unit
  def pop(): Option[T]
