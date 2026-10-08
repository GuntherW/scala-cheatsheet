package rag

import java.sql.{Connection, DriverManager}

/** JDBC-Verbindungsaufbau zur `postgres-rag`-Instanz (siehe `docker/docker-compose.yml`, Service `postgres-rag`, pgvector-Image, Port 5434, Datenbank `ragdb`) - eigener, von der sonstigen
  * Projekt-Infrastruktur (Service `postgres`, Port 5433) getrennter Container, damit dieses Lernprojekt dessen Datenbanken/Init-Skripte nicht anfasst.
  *
  * Verbindungsdaten sind bewusst nicht konfigurierbar/geheim gehalten (Default-Dev-Zugangsdaten aus `docker/.env`) - für Experimente lassen sie sich trotzdem per `.env` in diesem Projektordner
  * überschreiben (`PGRAG_URL`/`PGRAG_USER`/`PGRAG_PASSWORD`).
  */
object Db:

  private val url      = Env.getOrElse("PGRAG_URL", "jdbc:postgresql://localhost:5434/ragdb")
  private val user     = Env.getOrElse("PGRAG_USER", "postgres")
  private val password = Env.getOrElse("PGRAG_PASSWORD", "postgres")

  def connect(): Connection = DriverManager.getConnection(url, user, password)
