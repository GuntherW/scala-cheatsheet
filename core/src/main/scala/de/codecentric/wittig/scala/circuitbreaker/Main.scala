package de.codecentric.wittig.scala.circuitbreaker

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.circuitbreaker.{CircuitBreaker, CircuitBreakerConfig, CircuitBreakerRegistry}
import io.github.resilience4j.decorators.Decorators
import io.github.resilience4j.retry.Retry
import java.util.function.*

object Main extends App:

  private val circuitBreaker = CircuitBreaker.ofDefaults("backendService")

  // Create a Retry with default configuration
  // 3 retry attempts and a fixed time interval between retries of 500ms
  private val retry = Retry.ofDefaults("backendService")

  // Create a Bulkhead with default configuration
  private val bulkhead = Bulkhead.ofDefaults("backendService")

  private val supplier: Supplier[String] = () => myMethod()

  // Decorate your call to backendService.doSomething()
  // with a Bulkhead, CircuitBreaker and Retry
  // **note: you will need the resilience4j-all dependency for this
  val decoratedSupplier = Decorators
    .ofSupplier(supplier)
    .withCircuitBreaker(circuitBreaker)
    .withBulkhead(bulkhead)
    .withRetry(retry)
    .decorate

  def myMethod() = "Meine tolle Methode"

  println(decoratedSupplier.get())
