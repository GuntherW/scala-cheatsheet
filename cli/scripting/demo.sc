//> using lib "com.lihaoyi::pprint::0.7.1"

import pprint.*
import msg.guten
import constants.Messages

/** Aufruf mit:
  *  scala-cli . --main-class demo_sc
  *  */
pprintln("Hallo schönes pprint!")
println("Hallo normales print!")
println(s"Aus anderer Datei: $guten")
println(s"Aus Unterverzeichnis: ${Messages.messagec1}]")
