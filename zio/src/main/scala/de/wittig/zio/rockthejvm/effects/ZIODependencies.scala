package de.wittig.zio.rockthejvm.effects

import zio.*

import java.util.concurrent.TimeUnit

object ZIODependencies extends ZIOAppDefault:

  case class User(name: String, email: String)

  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase):
    def subscribeUser(user: User): Task[Unit] =
      for
        _ <- emailService.email(user)
        _ <- userDatabase.insert(user)
      yield ()
  object UserSubscription:
    def create(emailService: EmailService, userDatabase: UserDatabase)       = UserSubscription(emailService, userDatabase)
    def live: ZLayer[EmailService & UserDatabase, Nothing, UserSubscription] = ZLayer.fromFunction(create)

  class EmailService:
    def email(user: User): Task[Unit] =
      ZIO.succeed(println(s"you just been subscribed. Welcome ${user.name}"))
  object EmailService:
    def create                                   = new EmailService
    def live: ZLayer[Any, Nothing, EmailService] = ZLayer.succeed(create)

  class UserDatabase(connectionPool: ConnectionPool):
    def insert(user: User): Task[Unit] =
      for
        conn <- connectionPool.get
        _    <- conn.runQuery(s"insert into subscribers(name, email) values(${user.name}, ${user.email})")
      yield ()
  object UserDatabase:
    def create(connectionPool: ConnectionPool) = UserDatabase(connectionPool)
    def live                                   = ZLayer.fromFunction(create)

  class ConnectionPool(mConnections: Int):
    def get: Task[Connection] =
      ZIO.succeed(println("acquired connection")) *> ZIO.succeed(Connection())
  object ConnectionPool:
    def create(nConnections: Int) = ConnectionPool(nConnections)
    def live(nConnections: Int)   = ZLayer.succeed(create(nConnections))

  case class Connection():
    def runQuery(query: String): Task[Unit] =
      ZIO.succeed(println(s"executing query: $query"))

  private def subscribe(user: User) =
    for
      sub <- ZIO.service[UserSubscription]
      _   <- sub.subscribeUser(user)
    yield ()

  private val programm =
    for
      _ <- subscribe(User("gunther", "gunther@gunther.de"))
      _ <- subscribe(User("hans", "hans@gunther.de"))
    yield ()

  // ZLayer
  object ZLayerByHand {

    private val connectionPoolLayer: ULayer[ConnectionPool]                  = ZLayer.succeed(ConnectionPool.create(10))
    private val databaseLayer: ZLayer[ConnectionPool, Nothing, UserDatabase] = ZLayer.fromFunction(UserDatabase.create)
    private val emailServiceLayer: ULayer[EmailService]                      = ZLayer.succeed(EmailService.create)
    private val userSubscriptionServiceLayer                                 = ZLayer.fromFunction(UserSubscription.create)

    // vertical composition
    private val databaseLayerFull: ZLayer[Any, Nothing, UserDatabase] =
      connectionPoolLayer >>> databaseLayer

    // horizontal composition
    private val subscriptionRequirementsLayer: ZLayer[Any, Nothing, UserDatabase & EmailService] =
      databaseLayerFull ++ emailServiceLayer

    // mix & match
    private val userSubscriptionLayer: ZLayer[Any, Nothing, UserSubscription] =
      subscriptionRequirementsLayer >>> userSubscriptionServiceLayer

    private val userSubscriptionLayer2: ZLayer[Any, Nothing, UserSubscription] = ZLayer.make[UserSubscription](
      UserSubscription.live,
      EmailService.live,
      UserDatabase.live,
      ConnectionPool.live(10)
    )

    val runnableProgramm: ZIO[Any, Throwable, Unit] = programm
      .provide(userSubscriptionLayer)
  }

  private val runnableProgrammV2 = programm
    .provide(
      UserSubscription.live,
      EmailService.live,
      UserDatabase.live,
      ConnectionPool.live(10),
//      ZLayer.Debug.tree,    // will print graph to console
//      ZLayer.Debug.mermaid, // will provide a mermaid graph
    )

  /*
    Already provided services by ZIO:
      + Clock
      + Random
      + System
      + Console
   */
  private val getTime     = Clock.currentTime(TimeUnit.SECONDS)
  private val randomValue = Random.nextInt
  private val javaHome    = System.env("JAVA_HOME")
  private val printEffect = Console.printLine("Hallo Welt")

  def run = runnableProgrammV2
