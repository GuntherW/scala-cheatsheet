package de.codecentric.wittig.scala.xml

import com.thoughtworks.xstream.*
import com.thoughtworks.xstream.io.xml.DomDriver

case class Baum(name: String, hoehe: Int, wurzeltiefe: Int)

object XStream extends App:
  private val xStream = new XStream(new DomDriver)
  xStream
    .allowTypesByWildcard(Array("de.codecentric.wittig.**"))

  private val baum1       = Baum("Eiche", 20, 2)
  private val xml: String = xStream.toXML(baum1)
  println(xml)

  private val baum2 = xStream.fromXML(xml)
  println(baum2)
