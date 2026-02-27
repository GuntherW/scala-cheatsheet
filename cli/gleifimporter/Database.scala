import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import java.sql.Connection
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import os.Path
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

  private def withConnection[A](url: String, user: String, password: String)(f: Connection => A): A =
    val cfg = HikariConfig()
    cfg.setJdbcUrl(url)
    cfg.setUsername(user)
    cfg.setPassword(password)
    cfg.setMaximumPoolSize(1)
    val ds = HikariDataSource(cfg)
    try
      val conn = ds.getConnection()
      try f(conn)
      finally conn.close()
    finally ds.close()

  def insertChunk(chunk: Seq[LeiRecord], url: String, user: String, password: String): Int =
    withConnection(url, user, password): conn =>
      conn.setAutoCommit(false)
      val sql = """
        INSERT INTO gleif_lei_records (
          id, legal_name, legal_name_language, initial_registration_date,
          last_update_date, status, next_renewal_date, imported_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT (id) DO UPDATE SET
          legal_name                = EXCLUDED.legal_name,
          legal_name_language       = EXCLUDED.legal_name_language,
          initial_registration_date = EXCLUDED.initial_registration_date,
          last_update_date          = EXCLUDED.last_update_date,
          status                    = EXCLUDED.status,
          next_renewal_date         = EXCLUDED.next_renewal_date,
          imported_at               = CURRENT_TIMESTAMP
      """
      val stmt = conn.prepareStatement(sql)
      try
        for record <- chunk do
          stmt.setString(1, record.id)
          stmt.setString(2, record.legalName)
          stmt.setString(3, record.legalNameLanguage)
          stmt.setString(4, record.initialRegistrationDate)
          stmt.setString(5, record.lastUpdateDate)
          stmt.setString(6, record.status)
          stmt.setString(7, record.nextRenewalDate)
          stmt.addBatch()

        stmt.executeBatch()
        conn.commit()
        println(s"    Inserted ${chunk.size} records")
        chunk.size
      finally stmt.close()

  def insertRecords(records: List[LeiRecord], url: String, user: String, password: String): Unit =
    insertChunk(records, url, user, password)
    println(s"    Total: ${records.size} records inserted/updated")

  def verifyImport(url: String, user: String, password: String): Unit =
    withConnection(url, user, password): conn =>
      val stmt = conn.createStatement()
      try
        val total = stmt.executeQuery("SELECT COUNT(*) FROM gleif_lei_records")
        if total.next() then println(s"    Total LEI records: ${total.getInt(1)}")

        println("\n    Status distribution:")
        val byStatus = stmt.executeQuery(
          "SELECT status, COUNT(*) FROM gleif_lei_records GROUP BY status ORDER BY COUNT(*) DESC"
        )
        while byStatus.next() do
          println(s"      ${byStatus.getString(1)}: ${byStatus.getInt(2)}")
      finally stmt.close()

  def setupDatabase(configUrl: String, configUser: String, configPassword: String, migrationsPath: Path): Unit =
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
