//> using dep "org.postgresql:postgresql:42.6.0"

import java.sql.{Connection, DriverManager, ResultSet}

object CockroachDBExample extends App:
  // Connection details
  val url = "jdbc:postgresql://localhost:26257/movr?sslmode=disable"
  val user = "root"
  val password = "" // No password in insecure mode

  // Connect to CockroachDB
  val connection: Connection = DriverManager.getConnection(url, user, password)

  try
    // Create a statement
    val statement = connection.createStatement()

    // Execute a query
    val query = "SELECT id, city, name, address, credit_card FROM users LIMIT 10;"
    val resultSet: ResultSet = statement.executeQuery(query)

    // Print the results
    println("ID                                   | City       | Name                 | Address                        | Credit Card")
    println("-----------------------------------------------------------------------------------------------------------------------")
    while (resultSet.next()) do
      val id = resultSet.getString("id")
      val city = resultSet.getString("city")
      val name = resultSet.getString("name")
      val address = resultSet.getString("address")
      val creditCard = resultSet.getString("credit_card")
      println(f"$id%-36s | $city%-10s | $name%-20s | $address%-30s | $creditCard%-30s")

    // Clean up
    resultSet.close()
    statement.close()
  finally connection.close()
