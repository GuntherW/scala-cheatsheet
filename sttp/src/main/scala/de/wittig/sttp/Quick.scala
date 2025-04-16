package de.wittig.sttp
import sttp.client4.quick.*

object Quick extends App:

  println(quickRequest.get(uri"http://httpbin.org/ip").send())
