package de.wittig.metaprogramming

inline def fibonacci(n: Int): Int =
  if n <= 1 then n
  else fibonacci(n - 1) + fibonacci(n - 2)

object Fibonacci extends App:
  println(fibonacci(10)) // will compile to println(55)
