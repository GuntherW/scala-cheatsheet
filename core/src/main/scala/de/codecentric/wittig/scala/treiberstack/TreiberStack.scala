package de.codecentric.wittig.scala.treiberstack

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

class TreiberStack[T] extends Stack[T]:
  // Ein einfacher Knoten im Stack
  private case class Node(value: T, next: Option[Node])

  // Die "Spitze" des Stacks als atomare Referenz
  private val head = new AtomicReference[Option[Node]](None)

  // Element oben auflegen
  @tailrec
  final def push(value: T): Unit =
    val currentHead = head.get()
    val newNode     = Some(Node(value, currentHead))
    if !head.compareAndSet(currentHead, newNode) then push(value) // Retry bei Konflikt

  // Element von oben entfernen
  @tailrec
  final def pop(): Option[T] =
    head.get() match
      case None                 => None // Stack ist leer
      case currentHead @ Some(node) =>
        val nextNode = node.next
        if head.compareAndSet(currentHead, nextNode) then // Versuche atomar den Head auf den nächsten Knoten zu setzen
          Some(node.value)
        else
          pop() // Retry bei Konflikt

  // Hilfsmethode zum Leeren für den Test
  def popAll(): List[T] =
    @tailrec
    def loop(acc: List[T]): List[T] =
      pop() match
        case Some(v) => loop(v :: acc)
        case None    => acc
    loop(Nil)
