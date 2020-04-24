package de.codecentric.wittig.scala

trait BEIDE extends TA with TB {
  val a = new TAA()
  val b = new TBB()
}

trait TA {
  class TAA
}
trait TB {
  class TBB
}
