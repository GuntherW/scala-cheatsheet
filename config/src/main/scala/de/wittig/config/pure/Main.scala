package de.wittig.config.pure

import scala.util.chaining.scalaUtilChainingOps

import pureconfig.*
import pureconfig.generic.semiauto.*

@main
def main(): Unit =

  case class MyConf(pizza: Pizza) derives ConfigReader

  enum Pizza:
    case Pepperoni(tomatoes: Boolean = true)
    case Margherita(vege: Boolean)

//  given ConfigReader[Pizza] = deriveReader
//  given ConfigWriter[Pizza] = deriveWriter

  given ConfigConvert[Pizza] = deriveConvert // instead of deriving Reader and Writer separately

  // reading values
  ConfigSource.string("{ type: pepperoni }").load[Pizza].tap(println) // Right(Pepperoni(true))

  // writing values
  ConfigWriter[Pizza].to(Pizza.Pepperoni(tomatoes = false)).tap(println) // SimpleConfigObject({"tomatoes":false,"type":"pepperoni"})

  val resource = ConfigSource.resources("application.conf")
  resource.load[MyConf].tap(println)
  resource.at("pizza").load[Pizza].tap(println)
