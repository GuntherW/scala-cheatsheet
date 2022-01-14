package de.wittig.doobie

import cats.effect.*
import doobie.util.transactor.Transactor
import doobie.implicits.*
import doobie.{HC, HPS}
import doobie.util.update.Update

/** https://blog.rockthejvm.com/doobie/
  *
  * Start Postgres docker container with `cd docker` `docker-compose up`
  */
object DoobieDemo extends IOApp.Simple {

  case class Actor(id: Int, name: String)
  case class Movie(id: String, title: String, year: Int, actors: List[String], director: String)

  extension [A](io: IO[A]) {
    def debug: IO[A] = io.map { a =>
      println(s"[${Thread.currentThread().getName}] $a")
      a
    }
  }

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5432/myimdb",
    "postgres",
    "test"
  )

  def findAllActorNames: IO[List[String]] = {
    val query  = sql"select name from actors".query[String]
    val action = query.to[List]
    action.transact(xa)
  }

  def findActorById(id: Int): IO[Actor] = {
    val query  = sql"select id, name from actors where id=$id".query[Actor]
    val action = query.unique
    action.transact(xa)
  }

  val actorNamesStream: IO[List[String]] = sql"select name from actors".query[String]
    .stream
    .compile
    .toList
    .transact(xa)

  // HC, HPS
  def findActorByName(name: String): IO[Option[Actor]] = {
    val queryString = "select id, name from actors where name = ?"
    HC.stream[Actor](
      queryString,
      HPS.set(name),
      100
    ).compile
      .toList
      .map(_.headOption)
      .transact(xa)
  }

  // fragments
  def findActorsByInitial(letter: String): IO[List[Actor]] = {
    val selectPart = fr"select id, name"
    val fromPart   = fr"from actors"
    val wherePart  = fr"where LEFT(name,1) = $letter"
    val statement  = selectPart ++ fromPart ++ wherePart
    statement.query[Actor].stream.compile.toList.transact(xa)
  }

  // update
  def saveActor(id: Int, name: String): IO[Int] = {
    val query = sql"insert into actors (id, name) values ($id, $name)"
    query.update.run.transact(xa)
  }

  def saveActor_v2(id: Int, name: String): IO[Int] = {
    val queryString = "insert into actors(id, name) values (?, ?)"
    Update[Actor](queryString).run(Actor(id, name)).transact(xa)
  }

  // autogenerated IDs
  def saveActorAutoGenerated(name: String): IO[Int] = {
    sql"insert into actors (name) values($name)".update.withUniqueGeneratedKeys[Int]("id")
      .transact(xa)
  }

  // update/insert many
  def saveMultipleActors(actorNames: List[String]): IO[List[Actor]] = {
    val insertStatement = "insert into actors (name) values (?)"
    val updateAction    = Update[String](insertStatement).updateManyWithGeneratedKeys[Actor]("id", "name")(actorNames)
    updateAction.compile.toList.transact(xa)
  }

  val run: IO[Unit] =
    findActorById(1).debug.as(ExitCode.Success)
//    actorNamesStream.debug.as(ExitCode.Success)
//    findActorsByInitial("H").debug.as(ExitCode.Success)
//    saveActor(99, "Hotzenplotz").debug.as(ExitCode.Success)
//    saveActor_v2(33, "Hermann").debug.as(ExitCode.Success)
//    saveActorAutoGenerated("Tom").debug.as(ExitCode.Success)
//  saveMultipleActors(List("Alice", "Bob", "Charlie")).debug.as(ExitCode.Success)
}
