package de.wittig.database.duckdb

import org.duckdb.DuckDBConnection

import java.sql.{DriverManager, ResultSet}

object Main extends App:

//  val connection = DriverManager.getConnection("jdbc:duckdb:database/duck.db").asInstanceOf[DuckDBConnection]
  val connection = DriverManager.getConnection("jdbc:duckdb:").asInstanceOf[DuckDBConnection]
  val statement  = connection.createStatement()

  // Create a table
  val createTableSQL = """
                         CREATE TABLE users (
                           id INTEGER,
                           name VARCHAR,
                           age INTEGER
                         );
                         """
  statement.execute(createTableSQL)

  // Insert some data
  val insertDataSQL = """
                        INSERT INTO users (id, name, age) VALUES
                          (1, 'Alice', 30),
                          (2, 'Bob', 25),
                          (3, 'Charlie', 35);
                        """
  statement.execute(insertDataSQL)

  // Insert, using Prepared Statement
  val prepared = connection.prepareStatement("INSERT INTO users (id, name, age) VALUES (?, ?, ?)")
  prepared.setInt(1, 4)
  prepared.setString(2, "Hans")
  prepared.setInt(3, 44)
  prepared.execute()

  // Query the data
  val querySQL             = "SELECT * FROM users;"
  val resultSet: ResultSet = statement.executeQuery(querySQL)

  // Print the results
  println("ID | Name    | Age")
  println("-------------------")
  while resultSet.next() do
    val id   = resultSet.getInt("id")
    val name = resultSet.getString("name")
    val age  = resultSet.getInt("age")
    println(f"$id%2d | $name%-7s | $age%3d")

  // Clean up
  resultSet.close()
  statement.close()
  connection.close()
