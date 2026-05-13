package de.wittig.zioblocks.context

import zio.blocks.context.*

/** Context is a type-indexed heterogeneous collection that stores values by their types with compile-time type safety.
  */
@main def main(): Unit =
  case class Config(debug: Boolean)
  case class Metrics(count: Int)

  // Create a context with multiple values
  val ctx: Context[Config & Metrics] = Context(
    Config(debug = true),
    Metrics(count = 42)
  )

  // Retrieve values by type
  val config: Config   = ctx.get[Config]
  val metrics: Metrics = ctx.get[Metrics]

  // Add or update values
  val updated = ctx.update[Metrics](m => m.copy(count = m.count + 1))

  // Combine contexts
  val ctx1                              = Context(Config(false))
  val ctx2                              = Context(Metrics(0))
  val merged: Context[Config & Metrics] = ctx1 ++ ctx2
