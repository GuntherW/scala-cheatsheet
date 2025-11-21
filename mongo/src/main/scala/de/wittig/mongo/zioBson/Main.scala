package de.wittig.mongo.zioBson

import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters
import de.wittig.MongoUtil.mongoClient
import de.wittig.mongo.zioBson.Country.DE
import org.bson.UuidRepresentation
import org.bson.codecs.UuidCodecProvider
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import zio.bson
import zio.bson.*
import zio.schema.codec.BsonSchemaCodec
import zio.schema.{DeriveSchema, Schema}

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.collection.immutable.List

@main
def main(): Unit =

  given BsonCodec[Person]      = BsonSchemaCodec.bsonCodec(DeriveSchema.gen[Person])
  //  given BsonCodec[Person]      = BsonSchemaCodec.bsonCodec(DeriveSchema.gen[Person], Config.withSumTypeHandling(DiscriminatorField("type")))
  given BsonCodec[ContactInfo] = BsonSchemaCodec.bsonCodec(DeriveSchema.gen[ContactInfo])
  given BsonCodec[Address]     = BsonSchemaCodec.bsonCodec(DeriveSchema.gen[Address])

  val codecRegistry: CodecRegistry = fromRegistries(
    fromProviders(
      zioBsonCodecProvider[Person],
      zioBsonCodecProvider[ContactInfo],
      zioBsonCodecProvider[Address],
      new UuidCodecProvider(UuidRepresentation.STANDARD)
    ),
    MongoClientSettings.getDefaultCodecRegistry
  )

  val database = mongoClient
    .getDatabase("ziobson")
    .withCodecRegistry(codecRegistry)

  val personCollection  = database.getCollection("persons", classOf[Person])
  val addressCollection = database.getCollection("addresses", classOf[Address])

  val person = Person(
    id = UUID.randomUUID(),
    name = "John Doe",
    name1Opt = Some("some"),
    name2Opt = None,
    enum1 = Enum1.Enum1A("hallo"),
    sum1 = Sum1A("hallo"),
    age = 30,
    birthDate = LocalDate.of(1993, 5, 15),
    lastModified = Instant.now,
    active = true,
    tags = List("developer", "scala", "mongodb"),
    contactInfo = ContactInfo(
      phone = Some("+1-555-123-4567"),
      address = Address(
        street = "123 Main St",
        zipCode = "1234s",
        country = DE
      )
    )
  )

  try {
    personCollection.drop()
    addressCollection.drop()

    // Insert the person into the collection
    personCollection.insertOne(person)

    // Find the person by id
    val foundPerson = personCollection.find(Filters.eq("id", person.id)).first()
    println(s"Found person: $foundPerson")

    // Find all persons
    val allPersons = personCollection.find()
    println(s"All persons: $allPersons")

    // Insert an address directly
    val address = Address("456 Park Ave", "02108", DE)
    addressCollection.insertOne(address)

    // Find the address
    val foundAddress = addressCollection.find().first()
    println(s"Found address: $foundAddress")

  } finally
    if (mongoClient != null) mongoClient.close()

enum Country:
  case DE, US

enum Enum1:
  case Enum1A(wert: String)

sealed trait Sum1
case class Sum1A(wert: String) extends Sum1

case class Address(
    street: String,
    zipCode: String,
    country: Country
)

case class ContactInfo(
    phone: Option[String],
    address: Address
)

case class Person(
    id: UUID,
    name: String,
    name1Opt: Option[String],
    name2Opt: Option[String],
    enum1: Enum1,
    sum1: Sum1,
    age: Int,
    birthDate: LocalDate,
    lastModified: Instant,
    active: Boolean,
    tags: List[String],
    contactInfo: ContactInfo
)
