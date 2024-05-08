package de.wittig.server

import caliban.*
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.Schema.auto.*
import os.*

object CharacterDomain:

  // 1. Defining the schema
  enum Origin:
    case EARTH, MARS, BELT

  case class Character(name: String, nicknames: List[String], origin: Origin)
  case class CharactersArgs(origin: Option[Origin])
  case class CharacterArgs(name: String)

  // 2. Some data and business logic for our example
  private var sampleCharacters = List(
    Character("James Holden", List("Jim", "Hoss"), Origin.EARTH),
    Character("Naomi Nagata", Nil, Origin.BELT),
    Character("Amos Burton", Nil, Origin.EARTH),
    Character("Alex Kamal", Nil, Origin.MARS),
    Character("Chrisjen Avasarala", Nil, Origin.EARTH),
    Character("Josephus Miller", List("Joe"), Origin.BELT),
    Character("Roberta Draper", List("Bobbie", "Gunny"), Origin.MARS)
  )

  private val schema = render[Query]
  os.write.over(os.pwd / "caliban" / "schema.graphql", schema)
  println(schema)

  // Queries
  case class Query(
      characters: CharactersArgs => List[Character],
      character: CharacterArgs => Option[Character]
  )

  private def charactersByOrigin(origin: Option[Origin]): List[Character] =
    sampleCharacters.filter(c => origin.forall(_ == c.origin))

  private def characterByName(name: String): Option[Character] =
    sampleCharacters.find(_.name == name)

  // 3. Creating the resolver
  val characterQueryResolver: Query =
    Query(
      characters = args => charactersByOrigin(args.origin),
      character = args => characterByName(args.name)
    )

  // Mutations
  case class Mutations(
      deleteCharacter: CharacterArgs => Boolean,
      upperCaseCharacter: CharacterArgs => Boolean
  )

  val mutations = Mutations(
    deleteCharacter = args =>
      sampleCharacters = sampleCharacters.filterNot(_.name == args.name)
      true
    ,
    upperCaseCharacter = args =>
      sampleCharacters = sampleCharacters.map {
        case character if character.name == args.name => character.copy(name = character.name.toUpperCase)
        case c                                        => c
      }
      true
  )

object ServerMain extends App:
  import CharacterDomain.*
  import caliban.quick.*

  // 4. Turn the resolver into an API
  val api = graphQL(RootResolver(characterQueryResolver, mutations))

  // 5. Start a server for our API
  api.unsafe.runServer(8088, "/api/graphql")
