package de.codecentric.wittig.scala.treiberstack
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

object StackNodes:
  // Der Typ ist nur INNERHALB dieses Objects als Node[T] | Null bekannt
  opaque type NodeOpt[T] = Node[T] | Null

  case class Node[T](value: T, next: NodeOpt[T])

  // Factory-Methoden, damit der Code draußen sauber bleibt
  def empty[T]: NodeOpt[T]                              = null
  def create[T](value: T, next: NodeOpt[T]): NodeOpt[T] = Node(value, next)

  // Extension Methods für den Zugriff von außen
  extension [T](n: NodeOpt[T])
    def isDefined: Boolean = n != null
    def get: Node[T]       =
      if n == null then throw new NoSuchElementException("Empty Node")
      else n.asInstanceOf[Node[T]]

import StackNodes.*

class UltraFastTreiberStack[T] extends Stack[T]:
  // Nutze die Factory 'empty'
  private val head = new AtomicReference[NodeOpt[T]](StackNodes.empty)

  @tailrec
  final def push(value: T): Unit =
    val current = head.get()
    // Nutze die Factory 'create'
    val newNode = StackNodes.create(value, current)
    if !head.compareAndSet(current, newNode) then push(value)

  @tailrec
  final def pop(): Option[T] =
    val current = head.get()
    if !current.isDefined then None
    else
      val node = current.get
      if head.compareAndSet(current, node.next) then Some(node.value)
      else pop()
