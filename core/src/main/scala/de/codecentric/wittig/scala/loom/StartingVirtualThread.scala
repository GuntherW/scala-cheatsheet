package de.codecentric.wittig.scala.loom

@main
def startingVirtualThread(): Unit =

  val threads = Array.ofDim[Thread](10_000_000)

  for (i <- threads.indices)
    threads(i) = Thread.startVirtualThread(() => ())

  for (i <- threads.indices)
    threads(i).join()

  println("All threads finished")
