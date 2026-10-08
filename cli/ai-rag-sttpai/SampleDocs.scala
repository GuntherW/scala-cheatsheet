package rag

/** Inhalte der generierten Beispiel-PDFs (siehe `GenerateSampleDocs.scala`) - drei thematisch getrennte, kurze Fachtexte mit jeweils ein paar konkreten, eindeutig überprüfbaren Fakten. */
object SampleDocs:

  val photosynthese: List[String] =
    List(
      "Photosynthese: Grundlagen",
      "Photosynthese ist der biochemische Prozess, mit dem Pflanzen, Algen und einige Bakterien Lichtenergie in chemische Energie umwandeln. Dabei wird aus Kohlenstoffdioxid (CO2) und Wasser (H2O) unter Einwirkung von Lichtenergie Glukose (C6H12O6) und Sauerstoff (O2) gebildet.",
      "Die Gesamtgleichung der Photosynthese lautet: 6 CO2 + 6 H2O + Lichtenergie -> C6H12O6 + 6 O2. Dieser Prozess findet in den Chloroplasten statt, genauer in den Thylakoidmembranen (Lichtreaktion) und im Stroma (Calvin-Zyklus, Dunkelreaktion).",
      "Der grüne Farbstoff Chlorophyll absorbiert vor allem rotes und blaues Licht, reflektiert jedoch grünes Licht - das erklärt die grüne Farbe der meisten Pflanzenblätter. Es gibt zwei Hauptformen: Chlorophyll a und Chlorophyll b.",
      "Die Lichtreaktion findet in zwei gekoppelten Proteinkomplexen statt, Photosystem I und Photosystem II. Photosystem II spaltet dabei Wassermoleküle auf (Photolyse) und setzt molekularen Sauerstoff frei - dies ist die Quelle fast des gesamten Sauerstoffs in der Erdatmosphäre.",
      "Der Calvin-Zyklus (benannt nach dem Chemiker Melvin Calvin, Nobelpreis 1961) nutzt die in der Lichtreaktion erzeugten Energieträger ATP und NADPH, um CO2 über das Enzym RuBisCO in organische Moleküle einzubauen (Kohlenstofffixierung).",
      "C4-Pflanzen (z. B. Mais, Zuckerrohr) und CAM-Pflanzen (z. B. Kakteen) haben spezialisierte Varianten der Photosynthese entwickelt, um Wasserverlust in heißen/trockenen Klimazonen zu reduzieren und Photorespiration zu minimieren.",
    )

  val raumfahrt: List[String] =
    List(
      "Raumfahrt: Meilensteine und Fakten",
      "Der erste künstliche Erdsatellit, Sputnik 1, wurde am 4. Oktober 1957 von der Sowjetunion gestartet und löste den sogenannten 'Sputnik-Schock' sowie den Beginn des Wettlaufs ins All aus.",
      "Am 12. April 1961 wurde Juri Gagarin an Bord von Wostok 1 der erste Mensch im Weltraum. Sein Flug dauerte 108 Minuten und umrundete die Erde einmal vollständig.",
      "Am 20. Juli 1969 betraten Neil Armstrong und Buzz Aldrin im Rahmen der Apollo-11-Mission als erste Menschen den Mond. Armstrongs Satz 'Das ist ein kleiner Schritt für einen Menschen, aber ein riesiger Sprung für die Menschheit' wurde weltberühmt.",
      "Die Internationale Raumstation (ISS) ist seit November 2000 durchgehend bemannt und umkreist die Erde in etwa 400 Kilometern Höhe mit einer Geschwindigkeit von rund 27.600 km/h - das entspricht etwa 16 Erdumrundungen pro Tag.",
      "SpaceX, gegründet 2002 von Elon Musk, war 2012 das erste private Unternehmen, dessen Raumkapsel (Dragon) an die ISS andockte, und 2020 das erste private Unternehmen, das Astronauten der NASA ins All beförderte (Crew Dragon, Mission Demo-2).",
      "Die Falcon-9-Rakete von SpaceX war die erste orbitalfähige Rakete, deren erste Stufe routinemäßig wiederverwendet werden kann - sie landet dafür entweder auf einer Landeplattform im Meer oder zurück am Startplatz.",
      "Die Raumsonde Voyager 1, gestartet 1977, ist das am weitesten von der Erde entfernte von Menschen gebaute Objekt und verließ 2012 als erstes Raumfahrzeug den interstellaren Raum.",
    )

  val scala3: List[String] =
    List(
      "Scala 3: Wichtige Neuerungen",
      "Scala 3 (Codename 'Dotty') wurde im Mai 2021 veröffentlicht und bringt gegenüber Scala 2 eine grundlegend überarbeitete Compiler-Architektur sowie zahlreiche Sprachvereinfachungen mit.",
      "Eine der sichtbarsten Neuerungen ist die optionale, auf Einrückung basierende Syntax (significant indentation) als Alternative zu geschweiften Klammern - `if x then ... else ...` statt `if (x) { ... } else { ... }`.",
      "Scala 3 führt Union-Typen (`A | B`) und Intersection-Typen (`A & B`) als native Sprachfeatures ein, die es erlauben, Werte zu typisieren, die einer von mehreren bzw. mehreren Typen gleichzeitig entsprechen.",
      "Das Schlüsselwort `given`/`using` ersetzt in Scala 3 implizite Werte und Parameterlisten (`implicit val`/`implicit def` aus Scala 2) durch eine explizitere, besser lesbare Syntax für kontextuelle Abstraktion.",
      "Enums werden in Scala 3 als eigenständiges Sprachkonstrukt (`enum Color { case Red, Green, Blue }`) eingeführt und ersetzen das bisher übliche, umständlichere `sealed trait` + `case object`-Muster für einfache Aufzählungstypen.",
      "Metaprogrammierung erfolgt in Scala 3 über das neue `inline`-Schlüsselwort sowie Makros auf Basis von Quotes und Splices (`'{...}`/`${...}`) - das alte, als instabil geltende Scala-2-Makrosystem entfällt vollständig.",
      "Der neue Compiler (`dotc`) wurde von Grund auf neu geschrieben und bildet gleichzeitig die Grundlage für die TASTy-Zwischendarstellung (Typed Abstract Syntax Trees), die eine stabilere Binärkompatibilität zwischen Scala-3-Versionen ermöglicht.",
    )

  val all: List[(String, List[String])] = List(
    "photosynthese.pdf" -> photosynthese,
    "raumfahrt.pdf"     -> raumfahrt,
    "scala3.pdf"        -> scala3,
  )
