//> using javaOpt -Djava.net.preferIPv6Addresses=true
//> using toolkit 0.6.0

import sttp.client4.quick.*
import sttp.client4.Response
import scala.util.chaining.*

object Hallo extends App:
  quickRequest
    .get(uri"http://icanhazip.com")
    .send()
    .tap(res => println(res.body))
  quickRequest
    .get(uri"https://live.dad.cloud.otto.de/dad-view/jwks")
    .send()
    .tap(res => println(res.body))
