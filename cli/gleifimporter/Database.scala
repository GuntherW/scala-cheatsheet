//> using dep com.augustnagro::magnum:1.3.1
//> using dep org.postgresql:postgresql:42.7.10
//> using dep com.zaxxer:HikariCP:7.0.2
//> using dep org.flywaydb:flyway-core:12.0.3
//> using dep org.flywaydb:flyway-database-postgresql:12.0.3
//> using dep com.lihaoyi::os-lib:0.11.9-M6

import com.augustnagro.magnum.*
import com.augustnagro.magnum.SqlNameMapper.CamelToSnakeCase
import javax.sql.DataSource
import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import scala.util.chaining.scalaUtilChainingOps

object Database:

  case class LeiRecord(
      id: String,
      legalName: String,
      legalNameLanguage: String,
      initialRegistrationDate: String,
      lastUpdateDate: String,
      status: String,
      nextRenewalDate: String
  )

  private def parseTimestamp(iso: String): Timestamp =
    if iso == null || iso.isEmpty then null
    else Timestamp.from(Instant.parse(iso))

  private var cachedTransactor: Option[Transactor] = None

  private def getTransactor(url: String, user: String, password: String): Transactor =
    cachedTransactor.getOrElse:
      val xa = createTransactor(url, user, password)
      cachedTransactor = Some(xa)
      xa

  def insertChunk(chunk: Seq[LeiRecord], url: String, user: String, password: String): Int =
    val xa = getTransactor(url, user, password)
    transact(xa):
      batchUpdate(chunk): record =>
        val regDate = parseTimestamp(record.initialRegistrationDate)
        val updDate = parseTimestamp(record.lastUpdateDate)
        val renDate = parseTimestamp(record.nextRenewalDate)
        sql"""
          INSERT INTO gleif_lei_records (
            id, legal_name, legal_name_language, initial_registration_date,
            last_update_date, status, next_renewal_date
          ) VALUES (${record.id}, ${record.legalName}, ${record.legalNameLanguage}, $regDate, $updDate, ${record.status}, $renDate)
          ON CONFLICT (id) DO UPDATE SET
            legal_name                = EXCLUDED.legal_name,
            legal_name_language       = EXCLUDED.legal_name_language,
            initial_registration_date = EXCLUDED.initial_registration_date,
            last_update_date          = EXCLUDED.last_update_date,
            status                    = EXCLUDED.status,
            next_renewal_date         = EXCLUDED.next_renewal_date,
            imported_at               = CURRENT_TIMESTAMP
        """.update
    chunk.size

  def insertRecords(records: List[LeiRecord], url: String, user: String, password: String): Unit =
    insertChunk(records, url, user, password)
    println(s"    Total: ${records.size} records inserted/updated")

  def verifyImport(url: String, user: String, password: String): Unit =
    val xa = createTransactor(url, user, password)
    transact(xa):
      val totalVec = sql"SELECT COUNT(*) FROM gleif_lei_records".query[Long].run()
      val total    = totalVec.head
      println(s"✓ Total LEI records: $total")

      println("╔═══ Status Distribution ════╗")
      val byStatus = sql"SELECT status, COUNT(*) FROM gleif_lei_records GROUP BY status ORDER BY COUNT(*) DESC".query[(String, Long)].run()
      for (status, count) <- byStatus do
        val pct = (count.toDouble / total * 100).round
        println(s"  $status: $count ($pct%)")
      println("╚════════════════════════════╝")

  def setupDatabase(configUrl: String, configUser: String, configPassword: String, migrationsPath: os.Path): Unit =
    import org.flywaydb.core.Flyway
    import org.postgresql.ds.PGSimpleDataSource

    val ds = PGSimpleDataSource()
    ds.setURL(configUrl)
    ds.setUser(configUser)
    ds.setPassword(configPassword)

    Flyway
      .configure()
      .dataSource(ds)
      .locations(s"filesystem:$migrationsPath")
      .baselineOnMigrate(true)
      .load()
      .tap(_.repair())
      .tap(_.migrate())

  private def createTransactor(url: String, user: String, password: String): Transactor =
    val cfg = com.zaxxer.hikari.HikariConfig()
    cfg.setJdbcUrl(url)
    cfg.setUsername(user)
    cfg.setPassword(password)
    cfg.setMaximumPoolSize(4)
    val ds  = com.zaxxer.hikari.HikariDataSource(cfg)
    Transactor(ds)
