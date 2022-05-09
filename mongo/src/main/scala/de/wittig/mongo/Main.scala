package de.wittig.mongo
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq

object Main extends App {

  val uri = "mongodb://localhost:27017/?readPreference=primary&appname=MongoDB%20Compass&ssl=false"
  try {
    val mongoClient = MongoClients.create(uri)
    try {
      val database   = mongoClient.getDatabase("dad")
      val collection = database.getCollection("togglz")

      val doc = collection.find().first()
      System.out.println(doc.toJson)
    } finally if (mongoClient != null) mongoClient.close()
  }

}
