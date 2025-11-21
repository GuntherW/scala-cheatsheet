package de.wittig.sttp
import sttp.client4.quick.*

@main
def quick(): Unit =

  println(quickRequest.get(uri"http://httpbin.org/ip").send())
