package de.wittig.database.skunk

import java.util.UUID

import scala.util.chaining.*

import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.io.net.Network
import natchez.Trace
import natchez.Trace.Implicits.noop
import pureconfig.{ConfigReader, ConfigSource}
import skunk.*
import skunk.codec.all.*
import skunk.syntax.all.*

final case class User(
    id: UUID,
    name: String,
    email: String
)

final case class Config(
    host: String,
    port: Int,
    username: String,
    password: String,
    database: String
) derives ConfigReader

trait UserRepository[F[_]]:
  def createUser(user: User): F[Unit]
  def findByName(name: String): F[List[User]]

class UserRepositoryLive[F[_]: Concurrent: Console](session: Session[F]) extends UserRepository[F]:
  val codec: Codec[User] = (uuid, varchar, varchar).tupled.imap {
    case (id, name, email) => User(id, name, email)
  } {
    case User(id, name, email) => (id, name, email)
  }

  override def createUser(user: User): F[Unit] =
    for
      command  <- session.prepare(sql"insert into users values ($codec)".command)
      rowCount <- command.execute(user)
      _        <- Console[F].println(s"Inserted $rowCount rows into the db with $user")
    yield ()

  override def findByName(name: String): F[List[User]] =
    for
      query  <- session.prepare(sql"select * from users where name = $varchar".query(codec))
      result <- query.stream(name, 16).compile.toList
    yield result

object UserRepositoryLive:
  def apply[F[_]: Concurrent: Console](session: Session[F]): F[UserRepositoryLive[F]] = new UserRepositoryLive[F](session).pure[F]

/** https://blog.rockthejvm.com/skunk-complete-guide/
  *
  * Start Postgres docker container with cd docker docker-compose up docker exec -it postgres psql -U postgres \c skunkdb \dt
  */

object SkunkDemo extends IOApp.Simple:

  private def getSession[F[_]: Temporal: Trace: Network: Console](config: Config): Resource[F, Session[F]] =
    Session.single(
      host = config.host,
      port = config.port,
      user = config.username,
      password = Some(config.password),
      database = config.database
    )

  override def run: IO[Unit] =
    ConfigSource.default.at("db").load[Config] match
      case Left(error)   => IO.println(error)
      case Right(config) =>
        getSession[IO](config).use { session =>
          for
            repo     <- UserRepositoryLive[IO](session)
            rowCount <- repo.createUser(UUID.randomUUID().pipe(uuid => User(uuid, "peter", s"peter$uuid@peter.de")))
            users    <- repo.findByName("peter")
            _        <- users.traverse(IO.println)
          yield ()
        }
