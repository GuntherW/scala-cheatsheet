package de.wittig.mongo.java

import org.bson.Document
import de.wittig.MongoUtil.*

@main
def main(): Unit =

  try {
    val database   = mongoClient.getDatabase("test1")
    val collection = database.getCollection("gunther")
    collection.drop()

    val document = Document()
      .append("aKey", "aWert")
      .append("bKey", 2)
    collection.insertOne(document)
    val doc      = collection.find().first()
    println(doc.toJson)
  } finally if (mongoClient != null) mongoClient.close()
