package de.wittig.macros.inlines

inline def fibonacci(n: Int): Int =
  if n <= 1 then n
  else fibonacci(n - 1) + fibonacci(n - 2)

@main
def fibonacci(): Unit =
  println(fibonacci(10)) // will compile to println(55)
