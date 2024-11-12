package de.wittig.mongo.java

import com.mongodb.client.MongoClients

object Main extends App {

  private val uri         = "mongodb://localhost:27017/?readPreference=primary&appname=MongoDB%20Compass&ssl=false"
  private val mongoClient = MongoClients.create(uri)

  try {
    val database   = mongoClient.getDatabase("dad")
    val collection = database.getCollection("togglz")

    val doc = collection.find().first()
    System.out.println(doc.toJson)
  } finally if (mongoClient != null) mongoClient.close()
}
