package rag

import munit.FunSuite

class HashingTfIdfEmbedderTest extends FunSuite:

  private val corpus = List(
    "Katzen sind beliebte Haustiere und jagen gerne Mäuse",
    "Hunde sind treue Begleiter und lieben lange Spaziergänge",
    "Katzen jagen Mäuse und klettern gerne auf Bäume",
  )

  test("fitIdf liefert ein Modell mit korrekter Dimension und docCount"):
    val model = HashingTfIdfEmbedder.fitIdf(corpus, dims = 64)
    assertEquals(model.dims, 64)
    assertEquals(model.docCount, corpus.size)
    assertEquals(model.idf.length, 64)

  test("embed liefert einen Vektor mit der konfigurierten Dimension"):
    val model     = HashingTfIdfEmbedder.fitIdf(corpus, dims = 64)
    val embedding = HashingTfIdfEmbedder.embed(corpus.head, model)
    assertEquals(embedding.length, 64)

  test("embed liefert einen L2-normalisierten Vektor (Länge ~1) für nicht-leeren Text"):
    val model     = HashingTfIdfEmbedder.fitIdf(corpus, dims = 64)
    val embedding = HashingTfIdfEmbedder.embed(corpus.head, model)
    val norm      = math.sqrt(embedding.map(x => x.toDouble * x.toDouble).sum)
    assert(math.abs(norm - 1.0) < 1e-4, s"Erwartet L2-Norm ~1.0, war: $norm")

  test("embed liefert den Nullvektor für leeren Text"):
    val model     = HashingTfIdfEmbedder.fitIdf(corpus, dims = 64)
    val embedding = HashingTfIdfEmbedder.embed("   ", model)
    assert(embedding.forall(_ == 0f))

  test("thematisch ähnliche Texte sind sich kosinus-ähnlicher als thematisch verschiedene"):
    val model = HashingTfIdfEmbedder.fitIdf(corpus, dims = 256)

    def cosineSim(a: Array[Float], b: Array[Float]): Double =
      a.zip(b).map((x, y) => x.toDouble * y.toDouble).sum

    val katzenJagen = HashingTfIdfEmbedder.embed("Katzen jagen Mäuse", model)
    val katzenText1 = HashingTfIdfEmbedder.embed(corpus(0), model) // auch über Katzen/Mäuse
    val hundeText   = HashingTfIdfEmbedder.embed(corpus(1), model) // über Hunde/Spaziergänge

    val simToKatzen = cosineSim(katzenJagen, katzenText1)
    val simToHunde  = cosineSim(katzenJagen, hundeText)
    assert(simToKatzen > simToHunde, s"Erwartet höhere Ähnlichkeit zum Katzen-Text ($simToKatzen) als zum Hunde-Text ($simToHunde)")
