package de.wittig.macros.wartimizer

import de.wittig.macros.wartimizer.Wartimizer.wartimize

@main
def collectionOptimizerUsage(): Unit =

  val firstEven = List(1, 2, 3, 4, 5).filter(_ % 2 == 0).headOption

  // rewritten
  val firstEvenOptimized =
    wartimize(CollectionOptimizer)(List(1, 2, 3, 4, 5).filter(_ % 2 == 0).headOption)

  println(firstEven)
