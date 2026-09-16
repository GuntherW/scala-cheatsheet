package de.wittig.ox

import ox.flow.Flow

import java.nio.file.Paths

@main
def flowFileIO(): Unit =

  val resourcesDir = Paths.get(ClassLoader.getSystemResource("names.txt").toURI).getParent

  Flow
    .fromFile(resourcesDir.resolve("names.txt"))
    .linesUtf8
    .mapPar(4)(_.toLowerCase.capitalize)
    .intersperse("\n")
    .encodeUtf8
    .runToFile(resourcesDir.resolve("namesCapitalized.txt"))
