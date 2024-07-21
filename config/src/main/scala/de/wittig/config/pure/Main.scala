package de.wittig.config.pure

import pureconfig.*
import pureconfig.generic.semiauto.*

object Main extends App {

  enum Pizza:

    case Pepperoni(tomatoes: Boolean = true)
    case Margherita(vege: Boolean)

  given ConfigReader[Pizza] = deriveReader
  given ConfigWriter[Pizza] = deriveWriter

  given ConfigConvert[Pizza] = deriveConvert // instead of deriving Reader and Writer separately

  // reading values
  val pizza = ConfigSource.string("{ type: pepperoni }").load[Pizza]
  println(pizza) // Right(Pepperoni(true))

  // writing values
  val written = ConfigWriter[Pizza].to(Pizza.Pepperoni(tomatoes = false))
  println(written) // SimpleConfigObject({"tomatoes":false,"type":"pepperoni"})

}
