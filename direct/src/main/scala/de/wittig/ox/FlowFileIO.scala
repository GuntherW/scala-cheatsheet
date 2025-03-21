package de.wittig.ox

import ox.flow.Flow

import java.nio.file.Paths

object FlowFileIO extends App:

  Flow
    .fromFile(Paths.get("direct/src/main/resources/names.txt"))
    .linesUtf8
    .mapPar(4)(_.toLowerCase.capitalize)
    .intersperse("\n")
    .encodeUtf8
    .runToFile(Paths.get("direct/src/main/resources/namesCapitalized.txt"))
