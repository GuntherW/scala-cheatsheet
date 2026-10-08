package rag.ingestion

/** Inhalte der 10 generierten Beispiel-PDFs (siehe `GenerateSampleDocs.scala`) - zehn thematisch völlig unterschiedliche, kurze Fachtexte mit jeweils ein paar konkreten, eindeutig überprüfbaren
  * Fakten (Zahlen, Namen, Daten). Die bewusste Themenvielfalt erleichtert beim Ausprobieren der Pipeline zu beurteilen, ob die Vektorsuche tatsächlich das inhaltlich passende Dokument findet statt
  * irgendein beliebiges - bei nur einem Thema wäre jeder Treffer "irgendwie passend".
  */
object SampleDocs:

  val vulkane: List[String] =
    List(
      "Vulkane: Entstehung und bekannte Beispiele",
      "Vulkane entstehen dort, wo geschmolzenes Gestein (Magma) aus dem Erdmantel durch Risse in der Erdkruste an die Oberfläche gelangt. Man unterscheidet grob Schichtvulkane (steile Kegel aus Lava und Asche), Schildvulkane (flache, weitläufige Kegel aus dünnflüssiger Lava) und Calderen (große Einbruchskessel nach einem Ausbruch).",
      "Der Ausbruch des Vesuvs im Jahr 79 n. Chr. verschüttete die römischen Städte Pompeji und Herculaneum unter mehreren Metern Asche und Bimsstein - die Opfer und Gebäude blieben dadurch außergewöhnlich gut konserviert.",
      "Der Ausbruch des Tambora in Indonesien im Jahr 1815 gilt als der stärkste historisch dokumentierte Vulkanausbruch (Vulkanexplosivitätsindex 7) und führte 1816 weltweit zum sogenannten 'Jahr ohne Sommer' mit Missernten in Europa und Nordamerika.",
      "Island liegt auf dem Mittelatlantischen Rücken, wo die Eurasische und die Nordamerikanische Platte auseinanderdriften - deshalb zählt die Insel mit über 30 aktiven Vulkansystemen zu den vulkanisch aktivsten Regionen der Erde.",
      "Der Ring of Fire (Pazifischer Feuerring) umfasst rund 450 Vulkane entlang der Küsten des Pazifischen Ozeans und ist für etwa 75 % aller aktiven und ruhenden Vulkane weltweit verantwortlich.",
      "Der Ausbruch des Pinatubo auf den Philippinen 1991 schleuderte so viel Schwefeldioxid in die Stratosphäre, dass die globale Durchschnittstemperatur in den folgenden zwei Jahren um etwa 0,5 Grad Celsius sank.",
    )

  val schach: List[String] =
    List(
      "Schach: Geschichte und Regeln im Überblick",
      "Schach entwickelte sich vermutlich im 6. Jahrhundert n. Chr. in Indien aus dem Spiel Chaturanga und gelangte über Persien und die arabische Welt im Mittelalter nach Europa, wo es seine heutige Form erhielt.",
      "Ein Schachbrett besteht aus 64 Feldern in 8x8-Anordnung. Jede Partei beginnt mit 16 Steinen: einem König, einer Dame, zwei Türmen, zwei Läufern, zwei Springern und acht Bauern.",
      "Der jüngste Weltmeister der Schachgeschichte ist Garri Kasparow, der den Titel 1985 im Alter von 22 Jahren gewann. 1997 verlor er als amtierender Weltmeister eine vielbeachtete Partie gegen den IBM-Schachcomputer Deep Blue.",
      "Die Zugnotation folgt dem System der algebraischen Notation, bei dem jedes Feld durch eine Buchstaben-Zahlen-Kombination (z. B. e4 oder Sf3) eindeutig bezeichnet wird - a1 bis h8 aus Sicht der weißen Partei.",
      "Die 'Unsterbliche Partie' zwischen Adolf Anderssen und Lionel Kieseritzky (London, 1851) gilt als eine der berühmtesten Schachpartien überhaupt: Anderssen opferte Dame, beide Türme und einen Läufer, um am Ende dennoch zu gewinnen.",
      "2017 erreichte das von DeepMind entwickelte Programm AlphaZero nach nur 24 Stunden Selbstlernen (ohne menschliches Eröffnungswissen) Übermenschenniveau und schlug das bis dahin stärkste Schachprogramm Stockfish klar.",
    )

  val kaffee: List[String] =
    List(
      "Kaffee: Vom Anbau bis zur Tasse",
      "Der Kaffeestrauch stammt ursprünglich aus dem Hochland Äthiopiens. Einer bekannten Legende nach entdeckte ein Ziegenhirte namens Kaldi im 9. Jahrhundert die anregende Wirkung der Kaffeekirschen, als seine Ziegen nach deren Verzehr ungewöhnlich lebhaft wurden.",
      "Weltweit werden vor allem zwei Kaffeesorten angebaut: Arabica (etwa 60-70 % der Weltproduktion, milderer Geschmack, empfindlicher gegenüber Klima und Schädlingen) und Robusta (kräftiger, bitterer, höherer Koffeingehalt, robuster im Anbau).",
      "Brasilien ist seit über 150 Jahren der weltweit größte Kaffeeproduzent und erzeugt je nach Erntejahr etwa ein Drittel der globalen Kaffeemenge, gefolgt von Vietnam (vor allem Robusta) und Kolumbien.",
      "Eine Tasse Filterkaffee (etwa 200 ml) enthält im Schnitt rund 80-100 Milligramm Koffein, ein Espresso (etwa 30 ml) dagegen nur rund 60-80 Milligramm - Espresso wirkt pro Tasse also meist schwächer als angenommen, da die Trinkmenge deutlich kleiner ist.",
      "Die Röstung verändert die Bohne chemisch grundlegend: Durch die Maillard-Reaktion und die Karamellisierung von Zuckern entstehen über 800 verschiedene Aromastoffe, die für Geschmack und Duft des fertigen Kaffees verantwortlich sind.",
      "Finnland hat weltweit den höchsten Pro-Kopf-Kaffeeverbrauch mit durchschnittlich etwa 12 Kilogramm Rohkaffee pro Person und Jahr - das entspricht grob vier bis fünf Tassen täglich.",
    )

  val greatBarrierReef: List[String] =
    List(
      "Great Barrier Reef: Das größte Riffsystem der Erde",
      "Das Great Barrier Reef vor der Nordostküste Australiens erstreckt sich über etwa 2.300 Kilometer und bedeckt eine Fläche von rund 344.000 Quadratkilometern - es ist damit das größte zusammenhängende Korallenriffsystem der Welt und aus dem All mit bloßem Auge sichtbar.",
      "Das Riff besteht aus über 2.900 einzelnen Riffen und mehr als 900 Inseln, die von Millionen winziger Korallenpolypen über Jahrtausende aufgebaut wurden. Korallen sind festsitzende Nesseltiere, die in Symbiose mit photosynthetischen Algen (Zooxanthellen) leben.",
      "Seit 1981 steht das Great Barrier Reef als UNESCO-Weltnaturerbe unter Schutz und beherbergt eine außergewöhnliche Artenvielfalt: über 1.500 Fischarten, etwa 400 Korallenarten und 30 Walarten sowie Meeresschildkröten und Dugongs.",
      "Steigende Wassertemperaturen führen zur sogenannten Korallenbleiche: Die Korallen stoßen bei Hitzestress ihre symbiotischen Algen ab, verlieren dadurch ihre Farbe und oft auch ihre Nahrungsgrundlage. Zwischen 2016 und 2020 kam es zu vier großflächigen Bleichereignissen.",
      "Captain James Cook lief 1770 mit der HMS Endeavour auf einen Ausläufer des Riffs auf und musste das Schiff monatelang reparieren lassen - sein Bericht lieferte eine der ersten europäischen Beschreibungen des Riffsystems.",
      "Der Tourismus rund um das Riff erwirtschaftet jährlich mehrere Milliarden australische Dollar und unterstützt zehntausende Arbeitsplätze - gleichzeitig zählt der Tourismus selbst zu den Faktoren, deren Umweltbelastung streng reguliert wird.",
    )

  val roemischesReich: List[String] =
    List(
      "Das Römische Reich: Aufstieg und Fall",
      "Der Legende nach wurde Rom im Jahr 753 v. Chr. von Romulus gegründet. Aus der anfänglichen Monarchie entwickelte sich ab 509 v. Chr. die Römische Republik, die über Jahrhunderte durch gewählte Konsuln und den Senat regiert wurde.",
      "Mit Augustus, der 27 v. Chr. den Titel 'Princeps' annahm, begann die Zeit des Römischen Kaiserreichs. Augustus leitete die sogenannte Pax Romana ein, eine rund 200 Jahre andauernde Periode relativer innerer Stabilität im Reichsgebiet.",
      "Auf seinem Höhepunkt unter Kaiser Trajan (um 117 n. Chr.) erstreckte sich das Römische Reich über etwa 5 Millionen Quadratkilometer, von Britannien im Nordwesten bis Mesopotamien im Osten, mit schätzungsweise 50-90 Millionen Einwohnern.",
      "Das römische Straßennetz umfasste zu seiner Blütezeit über 80.000 Kilometer befestigte Straßen - der Ausspruch 'Alle Wege führen nach Rom' bezieht sich auf dieses Netz, das Handel, Verwaltung und Militärlogistik des Reichs ermöglichte.",
      "Im Jahr 395 n. Chr. wurde das Reich nach dem Tod von Kaiser Theodosius I. endgültig in ein West- und ein Oströmisches Reich geteilt. Das Weströmische Reich endete 476 n. Chr. mit der Absetzung des letzten Kaisers Romulus Augustulus durch Odoaker.",
      "Das Oströmische Reich (Byzantinisches Reich) mit der Hauptstadt Konstantinopel bestand noch fast 1.000 Jahre länger und fiel erst 1453 mit der Eroberung Konstantinopels durch die Osmanen unter Sultan Mehmed II.",
    )

  val bienen: List[String] =
    List(
      "Bienen: Lebensweise und Bedeutung für Ökosysteme",
      "Ein Bienenvolk der Honigbiene (Apis mellifera) besteht typischerweise aus einer Königin, mehreren hundert Drohnen (männliche Bienen) und bis zu 60.000 Arbeiterinnen - alle Arbeiterinnen sind genetisch weiblich, aber nicht geschlechtsreif.",
      "Eine Bienenkönigin kann über 2.000 Eier pro Tag legen und mehrere Jahre alt werden, während Arbeiterinnen im Sommer oft nur wenige Wochen leben, da sie sich durch Sammelflüge buchstäblich zu Tode arbeiten.",
      "Bienen kommunizieren die Entfernung und Richtung ergiebiger Futterquellen über den sogenannten Schwänzeltanz - eine Tanzfigur in Form einer liegenden Acht, die der Verhaltensforscher Karl von Frisch in den 1940er-Jahren entschlüsselte und dafür 1973 den Nobelpreis erhielt.",
      "Für die Erzeugung von nur einem Kilogramm Honig müssen Bienen schätzungsweise 3-4 Millionen Blüten besuchen und dafür insgesamt mehrere Zehntausend Kilometer zurücklegen.",
      "Rund 75 % der weltweit wichtigsten Nutzpflanzenarten (darunter viele Obst- und Gemüsesorten) profitieren zumindest teilweise von Insektenbestäubung, wobei Honig- und Wildbienen die wichtigste Bestäubergruppe stellen.",
      "Seit den 2000er-Jahren berichten Imker weltweit von ungewöhnlich hohen Völkerverlusten, bekannt als Colony Collapse Disorder - als mögliche Ursachen gelten eine Kombination aus Pestiziden, Parasiten (insbesondere der Varroamilbe), Lebensraumverlust und Krankheitserregern.",
    )

  val blockchain: List[String] =
    List(
      "Blockchain: Funktionsprinzip und Anwendungen",
      "Eine Blockchain ist eine verteilte, dezentral geführte Datenbank, in der Transaktionen in Blöcken gespeichert und kryptografisch mit dem jeweils vorherigen Block verkettet werden - jede nachträgliche Änderung eines Blocks würde alle folgenden Blöcke ungültig machen.",
      "Das im Jahr 2008 unter dem Pseudonym Satoshi Nakamoto veröffentlichte Bitcoin-Whitepaper beschrieb erstmals ein funktionierendes System, mit dem sich digitales Geld ohne zentrale Instanz (z. B. eine Bank) fälschungssicher übertragen lässt.",
      "Beim Proof-of-Work-Konsensmechanismus, den Bitcoin nutzt, müssen Teilnehmer ('Miner') rechenintensive kryptografische Rätsel lösen, um neue Blöcke anhängen zu dürfen - das macht nachträgliche Manipulationen praktisch unbezahlbar aufwendig.",
      "Die Ethereum-Blockchain führte 2015 sogenannte Smart Contracts ein: selbstausführende Programme, die direkt auf der Blockchain laufen und automatisch Bedingungen prüfen und Aktionen auslösen können, etwa bei dezentralen Finanzanwendungen (DeFi).",
      "Seit September 2022 nutzt Ethereum ('The Merge') statt Proof-of-Work den deutlich energiesparenderen Proof-of-Stake-Mechanismus, bei dem Teilnehmer durch das Hinterlegen (Staking) von Kryptowährung statt durch Rechenleistung am Konsens teilnehmen.",
      "Neben Kryptowährungen wird Blockchain-Technologie u. a. für die Nachverfolgung von Lieferketten, digitale Echtheitszertifikate (NFTs) und fälschungssichere Wahlsysteme diskutiert - wobei Skalierbarkeit und Energieverbrauch weiterhin zentrale Diskussionspunkte sind.",
    )

  val polarlichter: List[String] =
    List(
      "Polarlichter: Wie Nordlicht und Südlicht entstehen",
      "Polarlichter (Aurora borealis auf der Nordhalbkugel, Aurora australis auf der Südhalbkugel) entstehen, wenn geladene Teilchen des Sonnenwinds auf das Erdmagnetfeld treffen und entlang der Feldlinien in Richtung der Pole gelenkt werden.",
      "In der oberen Atmosphäre, in Höhen zwischen etwa 100 und 300 Kilometern, kollidieren diese Teilchen mit Sauerstoff- und Stickstoffmolekülen und regen sie zum Leuchten an - Sauerstoff erzeugt dabei meist grünes oder rötliches Licht, Stickstoff eher blaue und violette Töne.",
      "Die Häufigkeit und Intensität von Polarlichtern folgt dem etwa elfjährigen Sonnenfleckenzyklus: Während eines Sonnenfleckenmaximums nehmen Sonnenwind und Sonnenstürme zu, wodurch Polarlichter stärker und auch in niedrigeren Breiten sichtbar werden.",
      "Das sogenannte Carrington-Ereignis von 1859, ein extrem starker Sonnensturm, erzeugte Polarlichter, die bis in die Karibik sichtbar waren, und löste gleichzeitig Funkenflug und Ausfälle in den damaligen Telegrafennetzen aus.",
      "Gute Beobachtungschancen bestehen vor allem im sogenannten Polarlichtoval, etwa zwischen 60 und 75 Grad geografischer Breite - Regionen wie Nordnorwegen, Island, Alaska und Nordkanada gehören deshalb zu den beliebtesten Reisezielen für Polarlichtbeobachtung.",
      "Auch andere Planeten mit Magnetfeld und Atmosphäre, etwa Jupiter und Saturn, zeigen Polarlichter - die vom Hubble-Weltraumteleskop und der Raumsonde Juno aufgenommenen Polarlichter des Jupiters zählen zu den stärksten im Sonnensystem.",
    )

  val oper: List[String] =
    List(
      "Oper: Geschichte einer Kunstform",
      "Die Oper als Kunstform entstand um das Jahr 1600 in Florenz, als eine Gruppe von Gelehrten und Musikern (die 'Florentiner Camerata') versuchte, die Musikdramen der griechischen Antike wiederzubeleben. Als erste erhaltene vollständige Oper gilt 'Euridice' von Jacopo Peri (1600).",
      "Wolfgang Amadeus Mozart komponierte mit 'Die Zauberflöte' (1791) eine der bis heute meistgespielten Opern überhaupt - sie vereint Elemente der ernsten Oper mit volkstümlichem Humor und wurde kurz vor Mozarts Tod uraufgeführt.",
      "Giuseppe Verdi und Richard Wagner prägten im 19. Jahrhundert die Oper auf gegensätzliche Weise: Verdi perfektionierte die italienische Nummernoper mit eingängigen Arien (z. B. 'Aida', 'La Traviata'), während Wagner mit seinem Konzept des 'Gesamtkunstwerks' (u. a. im 'Ring des Nibelungen') durchkomponierte Musikdramen schuf.",
      "Die Mailänder Scala, eröffnet 1778, und die New Yorker Metropolitan Opera, eröffnet 1883, zählen bis heute zu den renommiertesten Opernhäusern der Welt und haben zahlreiche berühmte Uraufführungen erlebt.",
      "Giacomo Puccinis Oper 'Madama Butterfly' fiel bei ihrer Uraufführung 1904 in Mailand zunächst durch - nach Überarbeitung wurde sie wenige Monate später zu einem durchschlagenden Erfolg und zählt heute zu den meistgespielten Opern weltweit.",
      "Die Dauer einer Opernaufführung variiert stark: Wagners 'Götterdämmerung' dauert inklusive Pausen etwa 5-6 Stunden, während kürzere Werke wie Puccinis 'Gianni Schicchi' auf rund 50 Minuten kommen.",
    )

  val marathon: List[String] =
    List(
      "Marathonlauf: Von der Legende zum Massensport",
      "Der Name 'Marathon' geht auf die Legende des Boten Pheidippides zurück, der 490 v. Chr. nach der Schlacht von Marathon rund 40 Kilometer bis nach Athen gelaufen sein soll, um den Sieg über die Perser zu verkünden, und danach tot zusammengebrochen sei.",
      "Bei den ersten modernen Olympischen Spielen 1896 in Athen wurde erstmals ein Marathonlauf über etwa 40 Kilometer ausgetragen. Die heute gültige Standarddistanz von 42,195 Kilometern wurde erst bei den Olympischen Spielen 1908 in London festgelegt.",
      "Der aktuelle Marathon-Weltrekord der Männer liegt bei 2:00:35 Stunden, aufgestellt von Kelvin Kiptum beim Chicago-Marathon 2023 - der Weltrekord der Frauen liegt bei 2:09:56 Stunden, aufgestellt von Tigist Assefa beim Berlin-Marathon 2023.",
      "Die sogenannten 'World Marathon Majors' umfassen sechs besonders renommierte Stadtmarathons: Boston, London, Berlin, Chicago, New York City und Tokio. Der Boston-Marathon, erstmals 1897 ausgetragen, ist der älteste noch jährlich stattfindende Stadtmarathon der Welt.",
      "Viele Läufer berichten ab Kilometer 30-35 vom sogenannten 'Mann mit dem Hammer' - einem plötzlichen Energieeinbruch, der physiologisch meist auf die Erschöpfung der Glykogenspeicher in den Muskeln zurückgeführt wird.",
      "Der Berlin-Marathon gilt wegen seiner besonders flachen, schnellen Strecke als bevorzugter Rekordkurs - seit 1977 wurden dort mehr Marathon-Weltrekorde der Männer aufgestellt als bei jedem anderen Stadtmarathon.",
    )

  val all: List[(String, List[String])] = List(
    "vulkane.pdf"            -> vulkane,
    "schach.pdf"             -> schach,
    "kaffee.pdf"             -> kaffee,
    "great-barrier-reef.pdf" -> greatBarrierReef,
    "roemisches-reich.pdf"   -> roemischesReich,
    "bienen.pdf"             -> bienen,
    "blockchain.pdf"         -> blockchain,
    "polarlichter.pdf"       -> polarlichter,
    "oper.pdf"               -> oper,
    "marathon.pdf"           -> marathon,
  )
