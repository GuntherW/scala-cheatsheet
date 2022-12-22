//> using lib "com.lihaoyi::pprint::0.8.1"
//> using file "Weg2.scala"

import pprint.*
import weg1.guten
import constants.messages

/** Aufruf mit:
  *  scala-cli . --main-class demo_sc
  *  */
pprintln("Hallo schönes pprint!")
println("Hallo normales print!")
println(s"Aus anderer Datei (Weg1): $guten")
println(s"Aus anderer Datei (Weg2): ${Weg2.message}")
println(s"Aus Unterverzeichnis: ${messages.msg}")
