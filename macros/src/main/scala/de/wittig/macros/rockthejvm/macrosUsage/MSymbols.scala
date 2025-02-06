package de.wittig.macros.rockthejvm.macrosUsage
import de.wittig.macros.rockthejvm.macros.MSymbols.*
object MSymbols extends App:

  enum Permissions:
    case Read(bitset: Int, dir: String, mask: Boolean)
    case Denied

    private def changePermissions(b: Int, dir: String) = s"dir $dir has permissions $b"

    val bitMask = 0x34

  describeSymbols[Permissions]
