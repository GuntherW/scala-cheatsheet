package de.wittig.specific

import java.io.Closeable
import scala.util.Using
import com.softwaremill.macwire.*
import Dependencies.*

@main
def main(): Unit =
  val userStatusReader = Using.resource(DataSource("jdbc:h2:~/test")): ds =>
    autowire[UserStatusReader](
      ds,
      classOf[SecurityFilterImpl2],
      UserFinder(_, _, adminOnly = true)
    )

object Dependencies:

  class DataSource(jdbcConn: String) extends Closeable:
    def close(): Unit = ()

  class DatabaseAccess(ds: DataSource)

  trait SecurityFilter
  class SecurityFilterImpl  extends SecurityFilter
  class SecurityFilterImpl2 extends SecurityFilter

  class UserFinder(databaseAccess: DatabaseAccess, securityFilter: SecurityFilter, adminOnly: Boolean)

  class UserStatusReader(userFinder: UserFinder)
