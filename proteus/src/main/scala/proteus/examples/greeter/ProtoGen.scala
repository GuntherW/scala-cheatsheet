package proteus.examples.greeter

import proteus.*

object ProtoGen {
  def main(args: Array[String]): Unit =
    greeterService.renderToFile(Nil, "proteus/src/main/proto")
}
