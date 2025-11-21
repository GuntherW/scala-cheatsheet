package de.codecentric.wittig.scala.xml

import com.thoughtworks.xstream.*
import com.thoughtworks.xstream.io.xml.DomDriver

case class Baum(name: String, hoehe: Int, wurzeltiefe: Int)

@main
def xStream(): Unit =
  val xStream = new XStream(new DomDriver)
  xStream
    .allowTypesByWildcard(Array("de.codecentric.wittig.**"))

  val baum1       = Baum("Eiche", 20, 2)
  val xml: String = xStream.toXML(baum1)
  println(xml)

  val baum2 = xStream.fromXML(xml)
  println(baum2)
