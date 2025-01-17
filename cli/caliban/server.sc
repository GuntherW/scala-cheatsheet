//> using dep com.github.ghostdogpr::caliban-quick:2.9.1
//> using toolkit 0.6.0
//> using jvm 21

import caliban.*
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.Schema.auto.*
import os.*

// 1. Defining the schema
enum Origin {
  case EARTH, MARS, BELT
}

case class Character(name: String, nicknames: List[String], origin: Origin)
case class CharactersArgs(origin: Option[Origin])
case class CharacterArgs(name: String)

case class Query(
    characters: CharactersArgs => List[Character],
    character: CharacterArgs => Option[Character]
)

val schema = render[Query]
os.write.over(os.pwd / "schema.graphql", schema)
println(schema)

// 2. Some data and business logic for our example
val sampleCharacters = List(
  Character("James Holden", List("Jim", "Hoss"), Origin.EARTH),
  Character("Naomi Nagata", Nil, Origin.BELT),
  Character("Amos Burton", Nil, Origin.EARTH),
  Character("Alex Kamal", Nil, Origin.MARS),
  Character("Chrisjen Avasarala", Nil, Origin.EARTH),
  Character("Josephus Miller", List("Joe"), Origin.BELT),
  Character("Roberta Draper", List("Bobbie", "Gunny"), Origin.MARS)
)

def charactersByOrigin(origin: Option[Origin]): List[Character] =
  sampleCharacters.filter(c => origin.forall(_ == c.origin))

def characterByName(name: String): Option[Character] =
  sampleCharacters.find(_.name == name)

// 3. Creating the resolver
val queryResolver =
  Query(
    characters = args => charactersByOrigin(args.origin),
    character = args => characterByName(args.name)
  )

// 4. Turn the resolver into an API
val api = graphQL(RootResolver(queryResolver))

// 5. Start a server for our API
import caliban.quick.*
api.unsafe.runServer(8088, "/api/graphql")
