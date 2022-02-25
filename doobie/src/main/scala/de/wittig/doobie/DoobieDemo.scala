package de.wittig.doobie

import java.util.UUID

import cats.effect.*
import cats.implicits.*
import de.wittig.doobie.CustomSupport.ActorName
import doobie.util.transactor.Transactor
import doobie.implicits.*
import doobie.*
import doobie.util.update.Update
import doobie.util.{Get, Put, Read, Write}

/** https://blog.rockthejvm.com/doobie/
  *
  * Start Postgres docker container with `cd docker` `docker-compose up`
  */
object DoobieDemo extends IOApp.Simple:

  case class Actor(id: Int, name: String)
  case class Movie(id: String, title: String, year: Int, actors: List[String], director: String)

  extension [A](io: IO[A])
    def debug: IO[A] = io.map { a =>
      println(s"[${Thread.currentThread().getName}] $a")
      a
    }

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5432/myimdb",
    "postgres",
    "test"
  )

  def findAllActorNames: IO[List[String]] =
    val query  = sql"select name from actors".query[String]
    val action = query.to[List]
    action.transact(xa)

  def findActorById(id: Int): IO[Actor] =
    val query  = sql"select id, name from actors where id=$id".query[Actor]
    val action = query.unique
    action.transact(xa)

  val actorNamesStream: IO[List[String]] = sql"select name from actors".query[String]
    .stream
    .compile
    .toList
    .transact(xa)

  // HC, HPS
  def findActorByName(name: String): IO[Option[Actor]] =
    val queryString = "select id, name from actors where name = ?"
    HC.stream[Actor](
      queryString,
      HPS.set(name),
      100
    ).compile
      .toList
      .map(_.headOption)
      .transact(xa)

  // fragments
  def findActorsByInitial(letter: String): IO[List[Actor]] =
    val selectPart = fr"select id, name"
    val fromPart   = fr"from actors"
    val wherePart  = fr"where LEFT(name,1) = $letter"
    val statement  = selectPart ++ fromPart ++ wherePart
    statement.query[Actor].stream.compile.toList.transact(xa)

  // update
  def saveActor(id: Int, name: String): IO[Int] =
    val query = sql"insert into actors (id, name) values ($id, $name)"
    query.update.run.transact(xa)

  def saveActor_v2(id: Int, name: String): IO[Int] =
    val queryString = "insert into actors(id, name) values (?, ?)"
    Update[Actor](queryString).run(Actor(id, name)).transact(xa)

  // autogenerated IDs
  def saveActorAutoGenerated(name: String): IO[Int] =
    sql"insert into actors (name) values($name)".update.withUniqueGeneratedKeys[Int]("id")
      .transact(xa)

  // update/insert many
  def saveMultipleActors(actorNames: List[String]): IO[List[Actor]] =
    val insertStatement = "insert into actors (name) values (?)"
    val updateAction    = Update[String](insertStatement).updateManyWithGeneratedKeys[Actor]("id", "name")(actorNames)
    updateAction.compile.toList.transact(xa)

  // with Custom class
  def findAllActorNamesCustomClass =
    sql"select name from actors".query[ActorName].to[List].transact(xa)

  // large queries
  import doobie.postgres.*
  import doobie.postgres.implicits.*
  def findMovieByTitle(title: String): IO[Option[Movie]] =
    val statement =
      sql"""
        select m.id, m.title, m.year_of_production, array_agg(a.name) as actors, d.name || ' ' || d.last_name
        from movies m
        join movies_actors ma ON m.id = ma.movie_id
        join actors a ON ma.actor_id = a.id
        join directors d ON m.director_id = d.id
        where m.title = $title
        group by (m.id, m.title, m.year_of_production, d.name, d.last_name)
        """.query[Movie]

    statement.option.transact(xa)

  def findMovieByTitle2(title: String): IO[Option[Movie]] =
    def findMovieByTitle() =
      sql"select id, title, year_of_production, director_id from movies where title = $title"
        .query[(UUID, String, Int, Int)].option

    def findDirectorById(directorId: Int) =
      sql"select name, last_name from directors where id = $directorId"
        .query[(String, String)].option

    def findActorsByMovieId(movieId: UUID) =
      sql"""
        select a.name
        from actors a 
        join movies_actors ma on a.id = ma.actor_id
        where ma.movie_id = $movieId
      """
        .query[String]
        .to[List]

    val query =
      for
        maybeMovie    <- findMovieByTitle()
        maybeDirector <- maybeMovie match
                           case Some(_, _, _, directorId) => findDirectorById(directorId)
                           case None                      => Option.empty[(String, String)].pure[ConnectionIO]
        actors        <- maybeMovie match
                           case Some(movieId, _, _, _) => findActorsByMovieId(movieId)
                           case None                   => List.empty[String].pure[ConnectionIO]
      yield for
        (id, t, year, _)      <- maybeMovie
        (firstName, lastName) <- maybeDirector
      yield Movie(id.toString, t, year, actors, s"$firstName $lastName")

    query.transact(xa)

  val run: IO[Unit] =
    findMovieByTitle2("Zack Snyder's Justice League").debug.as(ExitCode.Success)
//    findMovieByTitle("Zack Snyder's Justice League").debug.as(ExitCode.Success)
//    findAllActorNamesCustomClass.debug.as(ExitCode.Success)
//    findActorById(1).debug.as(ExitCode.Success)
//    actorNamesStream.debug.as(ExitCode.Success)
//    findActorsByInitial("H").debug.as(ExitCode.Success)
//    saveActor(99, "Hotzenplotz").debug.as(ExitCode.Success)
//    saveActor_v2(33, "Hermann").debug.as(ExitCode.Success)
//    saveActorAutoGenerated("Tom").debug.as(ExitCode.Success)
//  saveMultipleActors(List("Alice", "Bob", "Charlie")).debug.as(ExitCode.Success)

object CustomSupport:

  // Class not under my control
  class ActorName(val value: String):
    override def toString: String = value

  object ActorName:
    implicit val actorNameGet: Get[ActorName] = Get[String].map(new ActorName(_))
    implicit val actorNamePut: Put[ActorName] = Put[String].contramap(_.value)

  case class DirectorId(id: Int)
  case class DirectorName(name: String)
  case class DirectorLastName(lastName: String)
  case class Director(id: DirectorId, name: DirectorName, lastName: DirectorLastName)
  object Director:
    implicit val directorRead: Read[Director]   = Read[(Int, String, String)].map {
      case (id, name, lastName) => Director(DirectorId(id), DirectorName(name), DirectorLastName(lastName))
    }
    implicit val directorWrite: Write[Director] = Write[(Int, String, String)].contramap {
      case Director(id, name, lastName) => (id.id, name.name, lastName.lastName)
    }
