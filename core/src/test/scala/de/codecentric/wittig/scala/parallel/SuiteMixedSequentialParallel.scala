package de.codecentric.wittig.scala.parallel

import org.scalatest.{ParallelTestExecution, Stepwise}
import scala.language.adhocExtensions

/** Does work. But executes [[TestInParallel]] and [[TestSequentially]] in parallel, also */
class SuiteMixedSequentialParallel
    extends Stepwise(
      new TestInParallel,
      new TestSequentially
    )
    with ParallelTestExecution
