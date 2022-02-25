package de.codecentric.wittig.scala.xml

import com.thoughtworks.xstream.*
import com.thoughtworks.xstream.io.xml.DomDriver

object XStream extends App:
  val xstream = new XStream(new DomDriver)

  val b           = Baum("Eiche", 20, 2)
  val xml: String = xstream.toXML(b)
  println(xml)

  val b2 = xstream.fromXML(xml)
  println(b2)

case class Baum(name: String, hoehe: Int, wurzeltiefe: Int)
