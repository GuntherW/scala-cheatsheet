package de.wittig.playground

/** https://www.inner-product.com/posts/direct-style-effects/?s=09 */

// For convenience, so we don't have to write Console.type everywhere.
type Console  = Console.type
type Print[A] = Console ?=> A

extension [A](print: Print[A]) {

  /** Insert a prefix before `print` */
  def prefix(first: Print[Unit]): Print[A] = Print {
    first
    print
  }

  /** Use red foreground color when printing */
  def red: Print[A] = Print {
    Print.print(Console.RED)
    val result = print
    Print.print(Console.RESET)
    result
  }
}

object Print:
  def print(msg: Any)(using c: Console): Unit   = c.print(msg)
  def println(msg: Any)(using c: Console): Unit = c.println(msg)

  def run[A](print: Print[A]): A =
    given c: Console = Console
    print

  /** Constructor for `Print` values */
  inline def apply[A](inline body: Console ?=> A): Print[A] = body

@main
def go(): Unit =
  // Declare some `Prints`
  val message: Print[Unit] = Print.println("Hello from direct-style land!")

  // Composition
  val red: Print[Unit] = Print.println("Amazing!").prefix(Print.print("> ").red)

  // Make some output
  Print.run(message)
  Print.run(red)
