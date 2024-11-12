package de.wittig

import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

package object database:

  def dataSource(db: DatabaseName): DataSource =
    val dataSource = new PGSimpleDataSource()
    dataSource.setServerNames(Array("localhost"))
    dataSource.setDatabaseName(db.name)
    dataSource.setUser("postgres")
    dataSource.setPassword("postgres")
    dataSource

  enum DatabaseName(val name: String):
    case MagnumDb extends DatabaseName("magnumdb")
    case SkunkDb  extends DatabaseName("skunkdb")
    case DoobieDb extends DatabaseName("myimdb")
    case TyquDb   extends DatabaseName("booksdb")
