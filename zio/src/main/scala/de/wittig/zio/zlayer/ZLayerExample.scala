package de.wittig.zio.zlayer
import de.wittig.zio.zlayer.ZLayerExample.UserDb.UserDbEnv
import de.wittig.zio.zlayer.ZLayerExample.UserEmailer.UserEmailerEnv
import de.wittig.zio.zlayer.ZLayerExample.UserSubscription.UserSubscriptionEnv
import zio._

/** Example from Rock the JVM
  * @see
  *   https://www.youtube.com/watch?v=PaogLRrYo64&list=PLmtsMNDRU0Bzu7NfhTiGK7iCYjcFAYlal
  */
object ZLayerExample extends zio.App {

  private val gunther = User("gunther", "gunther@gunther.de")
  case class User(name: String, email: String)

  object UserEmailer {
    type UserEmailerEnv = Has[UserEmailer.Service]

    // service definition
    trait Service {
      def notify(user: User, messages: String): Task[Unit]
    }

    // service implementation
    val live: ULayer[UserEmailerEnv] = ZLayer.succeed(new Service {
      override def notify(user: User, messages: String): Task[Unit] = Task {
        println(s"Sending $messages to ${user.email}")
      }
    })

    // front-facing API
    def notify(user: User, message: String): ZIO[UserEmailerEnv, Throwable, Unit] =
      ZIO.accessM(_.get.notify(user, message))
  }

  object UserDb {
    type UserDbEnv = Has[UserDb.Service]

    trait Service {
      def insert(user: User): Task[Unit]
    }

    val live: ULayer[UserDbEnv] = ZLayer.succeed(new Service {
      override def insert(user: User): Task[Unit] = Task {
        println(s"s[Database] insert into public.user values ('${user.email}')'")
      }
    })

    def insert(user: User): ZIO[UserDbEnv, Throwable, Unit] =
      ZIO.accessM(_.get.insert(user))
  }

  // Horizontal composition
  val userBackendLayer: ZLayer[Any, Nothing, UserDbEnv with UserEmailerEnv] = UserDb.live ++ UserEmailer.live

//  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
//    UserEmailer
//      .notify(gunther, "Willkommen")
//      .provideLayer(userBackendLayer)
//      .exitCode

  // Vertical composition
  object UserSubscription {
    type UserSubscriptionEnv = Has[UserSubscription.Service]
    class Service(notifier: UserEmailer.Service, userDb: UserDb.Service) {
      def subscribe(user: User): Task[User] =
        for {
          _ <- notifier.notify(gunther, s"Wilkommen ${user.name}")
          _ <- userDb.insert(user)
        } yield user
    }

    val live: ZLayer[UserEmailerEnv with UserDbEnv, Nothing, UserSubscriptionEnv] =
      ZLayer.fromServices[UserEmailer.Service, UserDb.Service, UserSubscription.Service] { (userEmailer, userDb) =>
        new Service(userEmailer, userDb)
      }

    // front facing API
    def subscribe(user: User): ZIO[UserSubscriptionEnv, Throwable, User] =
      ZIO.accessM(_.get.subscribe(user))
  }

  val userSubscriptionLayer: ZLayer[Any, Nothing, UserSubscriptionEnv] = userBackendLayer >>> UserSubscription.live

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    UserSubscription
      .subscribe(gunther)
      .provideLayer(userSubscriptionLayer)
      .exitCode
}
