package de.wittig.database.duckdb

import java.sql.{Connection, DriverManager}
import scala.util.Using

case class User(id: Int, name: String, age: Int)

@main
def main(): Unit =
  Using.resource(DriverManager.getConnection("jdbc:duckdb:")) { connection =>
    given Connection = connection

    createTable()
    insertUsers(List(User(1, "Alice", 30), User(2, "Bob", 25), User(3, "Charlie", 35)))
    insertUser(User(4, "Hans", 44))
    val users = queryUsers()
    println("ID | Name    | Age")
    println("-------------------")
    users.foreach(user => println(f"${user.id}%2d | ${user.name}%-7s | ${user.age}%3d"))
  }

private def createTable()(using connection: Connection): Unit =
  val createTableSQL =
    """CREATE TABLE users (
      |  id INTEGER,
      |  name VARCHAR,
      |  age INTEGER
      |);""".stripMargin
  Using.resource(connection.createStatement())(_.execute(createTableSQL))

private def insertUsers(users: List[User])(using connection: Connection): Unit =
  Using.resource(connection.prepareStatement("INSERT INTO users (id, name, age) VALUES (?, ?, ?)")) { prepared =>
    users.foreach { user =>
      prepared.setInt(1, user.id)
      prepared.setString(2, user.name)
      prepared.setInt(3, user.age)
      prepared.addBatch()
    }
    prepared.executeBatch()
  }

private def insertUser(user: User)(using connection: Connection): Unit =
  Using.resource(connection.prepareStatement("INSERT INTO users (id, name, age) VALUES (?, ?, ?)")) { prepared =>
    prepared.setInt(1, user.id)
    prepared.setString(2, user.name)
    prepared.setInt(3, user.age)
    prepared.execute()
  }

private def queryUsers()(using connection: Connection): Seq[User] =
  Using.Manager { use =>
    val statement = use(connection.createStatement())
    val resultSet = use(statement.executeQuery("SELECT * FROM users;"))
    Iterator
      .continually(resultSet)
      .takeWhile(_.next())
      .map(rs => User(rs.getInt("id"), rs.getString("name"), rs.getInt("age")))
      .toSeq
  }.get
