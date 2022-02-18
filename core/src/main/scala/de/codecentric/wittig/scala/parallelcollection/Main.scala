package de.codecentric.wittig.scala.parallelcollection
import scala.collection.parallel.CollectionConverters.*

/** You need to include "org.scala-lang.modules" %% "scala-parallel-collections"
  */
object Main extends App:

  List
    .tabulate(1000)(identity)
    .par
    .map(_ + 1)
    .foreach(println)
