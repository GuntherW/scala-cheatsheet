import com.dimafeng.testcontainers.{ForAllTestContainer, MongoDBContainer}
import org.mongodb.scala.*
import org.scalatest.matchers.should.Matchers
import scala.concurrent.Await
import scala.concurrent.duration.*

import org.scalatest.funsuite.AnyFunSuite
import org.testcontainers.utility.DockerImageName

class MongoDBContainerTest extends AnyFunSuite with ForAllTestContainer with Matchers {

  // Define the MongoDB container
  override val container: MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:7.0"))

  // Create MongoDB client using the container's connection string
  lazy val mongoClient: MongoClient = MongoClient(container.container.getConnectionString)
  lazy val database: MongoDatabase  = mongoClient.getDatabase("test-db")

  ignore("start and connect to MongoDB") {
    // Ensure that the database is reachable and functional
    val collection: MongoCollection[Document] = database.getCollection("test-collection")

    // Insert a document into the collection
    val insertObservable = collection.insertOne(Document("name" -> "test", "value" -> 123))

    // Block until the insert operation completes (not recommended for production)
    Await.result(insertObservable.toFuture(), 10.seconds)

    // Verify that the document is inserted
    val findObservable = collection.find(Document("name" -> "test")).first()

    val foundDocument = Await.result(findObservable.toFuture(), 10.seconds)
    foundDocument.get("name") shouldEqual Some("test")
    foundDocument.get("value") shouldEqual Some(123)
  }
}
