package de.wittig.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.scala.{DE, EN, ScalaDsl, Scenario}
import io.cucumber.scala.Implicits.ScalaDataTable
import org.junit.Assert.*

class TabellenStepDefinitions extends ScalaDsl with DE {

  var quellListe: List[Seq[Option[String]]] = Nil
  var istListe: List[Int]                   = Nil

  Angenommen("""Ich habe die Wertetabelle""") { (table: DataTable) =>
    quellListe = table.asScalaLists.toList
  }

  Wenn("""ich diese Werte jeweils addiere""") { () =>
    istListe = quellListe.collect {
      case Some(a) :: Some(b) :: Nil => Calculator.add(a.toInt, b.toInt)
    }
  }

  Dann("erhalte ich die Werte") { (table: DataTable) =>
    val sollListe = table.asScalaList.toList.flatten.map(_.toInt)
    assertEquals(sollListe, istListe)
  }
}
