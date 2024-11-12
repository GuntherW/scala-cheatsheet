package de.wittig.mongo.javadriver

import com.dimafeng.testcontainers.{ForAllTestContainer, MongoDBContainer}
import com.mongodb.client.{MongoClient, MongoClients, MongoDatabase}
import org.bson.Document
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.testcontainers.utility.DockerImageName

class MongoDBContainerTest extends AnyFunSuite with ForAllTestContainer with Matchers {

  // Define the MongoDB container
  override val container: MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:8.0"))

  // Create MongoDB client using the container's connection string
  lazy val mongoClient: MongoClient = MongoClients.create(container.container.getConnectionString)
  lazy val database: MongoDatabase  = mongoClient.getDatabase("test-db")

  test("start and connect to MongoDB") {
    // Ensure that the database is reachable and functional
    val collection = database.getCollection("test-collection")

    // Insert a document into the collection
    val doc: Document    = new Document("name", "test").append("value", 123)
    val insertObservable = collection.insertOne(doc)

    // Verify that the document is inserted
    val foundDocument = collection.find(Document("name", "test")).first()

    foundDocument.getString("name") shouldEqual "test"
    foundDocument.getInteger("value") shouldEqual 123
  }
}
