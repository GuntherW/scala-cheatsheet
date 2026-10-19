package rag

object PgConfig:

  val host: String     = sys.env.getOrElse("PGRAG_HOST", "localhost")
  val port: Int        = sys.env.getOrElse("PGRAG_PORT", "5434").toInt
  val database: String = sys.env.getOrElse("PGRAG_DATABASE", "ragdb")
  val user: String     = sys.env.getOrElse("PGRAG_USER", "postgres")
  val password: String = sys.env.getOrElse("PGRAG_PASSWORD", "postgres")

  val jdbcUrl: String = s"jdbc:postgresql://$host:$port/$database"

  // text-embedding-3-small: 1536 Dimensionen
  val table: String = "llm4s_pdf_chunks"
