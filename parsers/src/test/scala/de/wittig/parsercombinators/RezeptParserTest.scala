package de.wittig.parsercombinators

import munit.FunSuite

class RezeptParserTest extends FunSuite:

  test("Name"):
    val name = RezeptParser.parse(RezeptParser.name, "Name: Maibock").get
    assert(name == "Maibock")

  test("Liter"):
    val liter = RezeptParser.parse(RezeptParser.liter, " 33l").get
    assert(liter == 33)

  test("Menge in l"):
    val menge = RezeptParser.parse(RezeptParser.menge, "Menge: 33l").get
    assert(menge == 33)

  test("Menge in Liter"):
    val menge = RezeptParser.parse(RezeptParser.menge, "Menge: 33 Liter").get
    assert(menge == 33)

  test("Alpha"):
    val alpha = RezeptParser.parse(RezeptParser.alphasaeure, "13,5% Alpha").get
    assertEquals(alpha, 13.5)

  test("Schüttung"):
    val alpha = RezeptParser.parse(
      RezeptParser.schuettung,
      """Schüttung:
           Pilsener Malz 4000g
           Münchener Malz 2000g"""
    ).get
    assertEquals(
      alpha,
      List(
        Schuettung(malzname = "Pilsener Malz", menge = 4000),
        Schuettung(malzname = "Münchener Malz", menge = 2000)
      )
    )

  test("Maibock") {
    val maibockRezept = """
      Name: Maibock
      Menge: 22l
      Schüttung:
        Pilsener Malz 4000g
        Münchener Malz 2000g
      Brauwasser:
        20l Hauptguss
        13l Nachguss
      Hopfen:
        Spalter Select 5,3% Alpha 50g
        Magnum 13,5% Alpha 10g
        Cascade 5,0% Alpha 23g
      Hefe: #Kommentar, der geskipped wird#
        Untergärige Trockenhefe Saflager W34/70
      Maischen:
        Einmaischen 50°C
        1. Rast bei 57°C für 10 Min
        2. Rast bei 63°C für 60 Min
        Abläutern 78°C"""

    RezeptParser(maibockRezept) match
      case RezeptParser.Success(mb, _)    =>
        println(s"Success: $mb")
        assert(mb.name == "Maibock")
        assert(mb.menge == Some(22))
        assert(mb.schuettung.size == 2)
        assert(mb.schuettung(0).malzname == "Pilsener Malz")
        assert(mb.schuettung(0).menge == 4000)
        assert(mb.schuettung(1).malzname == "Münchener Malz")
        assert(mb.schuettung(1).menge == 2000)
        assert(mb.brauwasser.hauptguss == 20)
        assert(mb.brauwasser.nachguss == 13)
        assert(mb.hopfen(0).name == "Spalter Select")
        assert(mb.hopfen(2).name == "Cascade")
        assert(mb.hefe.startsWith("Untergärige Tro"))
        assert(mb.maischvorgang.einmaischen == 50)
        assert(mb.maischvorgang.rasten(0) == Rast(1, 10, 57))
        assert(mb.maischvorgang.rasten(1) == Rast(2, 60, 63))
        assert(mb.maischvorgang.ablaeutern == 78)
      case RezeptParser.NoSuccess(msg, r) =>
        println(s"Fehler: $msg => $r")
        fail(msg)
  }
