package de.codecentric.wittig.scala.xml

import scala.xml.*
import scala.xml.transform.{RewriteRule, RuleTransformer}

object ScalaXml extends App:

  val books =
    <books>
      <book id="b1615">Don Quixote</book>
      <book id="b1867">War and Peace</book>
    </books>

  val ids    = (books \ "book").map(book => book \@ "id").toList
  val titles = (books \ "book").map(book => book.text).toList
  titles.foreach(println)
  ids.foreach(println)

  // Filtern auf Attribute
  val quixote = (books \ "book").find(book => (book \@ "id") == "b1615").map(_.text)
  println(quixote) // Filtern auf Attribute

  // Filtern auf Text
  val quixote2 = (books \ "book").find(_.text == "Don Quixote").map(_.text)
  println(quixote2)

  // Load from a String
  val a = XML.loadString("""<book id="b1615">Don Quixote</book>""")
  println(a \@ "id")

  // Rewrite XML
  val abbreviateDayRule: RewriteRule = new RewriteRule:
    override def transform(n: Node): Seq[Node] =
      n match
        case elem: Elem if elem.label == "book" =>
          elem.copy(child = elem.child collect { case Text(data) =>
            Text(data.take(3))
          })
        case n                                  => n

  val transform   = new RuleTransformer(abbreviateDayRule)
  val transformed = transform(books)
  println(transformed)
