package de.wittig.database.tyqu

import tyqu.*

object BookDatabase:

  object Books extends Table:
    val id       = Column[Int](primary = true)
    val title    = Column[String]()
    val authorId = Column[Int]()

    lazy val author = ManyToOne(Authors, authorId)

  object Authors extends Table:
    val id        = Column[Int](primary = true)
    val firstName = Column[String]()
    val lastName  = Column[String]()
    val birthYear = Column[Int]()

    lazy val books = OneToMany(Books, Books.author)
