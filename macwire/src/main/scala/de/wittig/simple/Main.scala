package de.wittig.simple

import com.softwaremill.macwire.*
import Dependencies.*

@main
def main(): Unit =
  val userStatusReader = autowire[UserStatusReader]()

object Dependencies:
  class DatabaseAccess:
    def printHallo(): Unit = println("Hallo")
  class SecurityFilter
  class UserFinder(databaseAccess: DatabaseAccess, securityFilter: SecurityFilter)
  class UserStatusReader(userFinder: UserFinder)
