package de.wittig

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

package object database:

  def dataSource(db: DatabaseName): DataSource =
    val dataSource = new PGSimpleDataSource()
    dataSource.setServerNames(Array("localhost"))
    dataSource.setPortNumbers(Array(5433))
    dataSource.setDatabaseName(db.name)
    dataSource.setUser("postgres")
    dataSource.setPassword("postgres")
    dataSource

  def hikariDataSource(db: DatabaseName): DataSource =

    val hikariConfig = new HikariConfig()
    hikariConfig.setDataSource(dataSource(db))
    hikariConfig.setMaximumPoolSize(10)      // Set pool size, e.g., 10
    hikariConfig.setConnectionTimeout(30000) // Optional: Set connection timeout
    hikariConfig.setIdleTimeout(600000)      // Optional: Set idle timeout

    // Initialize HikariDataSource with this configuration
    new HikariDataSource(hikariConfig)

  // Now `hikariDataSource` can be used as your datasource.
  enum DatabaseName(val name: String):
    case MagnumDb extends DatabaseName("magnumdb")
    case SkunkDb  extends DatabaseName("skunkdb")
    case DoobieDb extends DatabaseName("myimdb")
    case TyquDb   extends DatabaseName("booksdb")
