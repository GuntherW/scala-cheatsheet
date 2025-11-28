package de.wittig.macros.typesafejdbc

import java.sql.*
import scala.collection.mutable.ListBuffer

object JdbcCommunication:

  private def withConnection[A](f: Connection => A) =
    Class.forName("org.postgresql.Driver") // load the driver

    // get a connection
    val connection = DriverManager.getConnection("jdbc:postgresql://localhost:5433/macro", "postgres", "postgres")
    try
      f(connection)
    finally
      connection.close()

  private def parseRows(schema: Schema, resultSet: ResultSet): List[Row] =
    val result = ListBuffer.empty[Row]
    while (resultSet.next()) {
      val row = schema.values.map {
        descriptor =>
          val value = resultSet.getObject(descriptor.index) match
            case array: java.sql.Array => array.getArray()
            case obj                   => obj
          descriptor.name -> value
      }
      result += Row(row.toMap)
    }
    result.toList

  def getSchema(query: String): Schema =
    withConnection { connection =>
      // create a PreparedStatment
      val statement = connection.prepareStatement(query)

      // get metadata out of that PreparedStatement
      val metadata = statement.getMetaData

      // transform metadata into Schema
      Schema.fromMetadata(metadata)
    }

  def runQuery(query: String): List[Row] =
    withConnection: connection =>
      val statement = connection.createStatement()
      val result    = statement.executeQuery(query)
      val metaData  = result.getMetaData
      val schema    = Schema.fromMetadata(metaData)
      parseRows(schema, result)
