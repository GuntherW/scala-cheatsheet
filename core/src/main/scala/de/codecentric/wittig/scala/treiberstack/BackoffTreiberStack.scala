package de.codecentric.wittig.scala.treiberstack

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport
import scala.annotation.tailrec

class BackoffTreiberStack[T]:
  private case class Node(value: T, next: Option[Node])
  private val head = new AtomicReference[Option[Node]](None)

  // Konfiguration für das Backoff
  private val MinBackoff = 10   // Nanosekunden
  private val MaxBackoff = 1000 // Nanosekunden

  def push(value: T): Unit =
    @tailrec
    def retry(currentBackoff: Int): Unit =
      val currentHead = head.get()
      val newNode     = Some(Node(value, currentHead))

      if !head.compareAndSet(currentHead, newNode) then
        // Fehlschlag! Wir warten kurz, bevor wir es erneut versuchen
        val sleepTime = ThreadLocalRandom.current().nextInt(currentBackoff)
        LockSupport.parkNanos(sleepTime)
        LockSupport.parkNanos(sleepTime)

        // Exponentiell steigern, aber bei MaxBackoff deckeln
        retry(Math.min(currentBackoff * 2, MaxBackoff))

    retry(MinBackoff)

  def pop(): Option[T] =
    @tailrec
    def retry(currentBackoff: Int): Option[T] =
      val currentHead = head.get()
      currentHead match
        case None                 => None
        case oldHead @ Some(node) =>
          if head.compareAndSet(oldHead, node.next) then
            Some(node.value)
          else
            val sleepTime = ThreadLocalRandom.current().nextInt(currentBackoff)
            LockSupport.parkNanos(sleepTime)
            retry(Math.min(currentBackoff * 2, MaxBackoff))

    retry(MinBackoff)
