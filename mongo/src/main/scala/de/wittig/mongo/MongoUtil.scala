package de.wittig

import com.mongodb.client.MongoClients

object MongoUtil:

  val uri         = "mongodb://localhost:27117/?readPreference=primary&appname=MongoDB%20Compass&ssl=false"
  val mongoClient = MongoClients.create(uri)
