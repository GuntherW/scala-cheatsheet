package de.wittig.zio.zlayer
import de.wittig.zio.zlayer.ZLayerExample.UserDb.UserDbEnv
import de.wittig.zio.zlayer.ZLayerExample.UserEmailer.UserEmailerEnv
import de.wittig.zio.zlayer.ZLayerExample.UserSubscription.UserSubscriptionEnv
import zio.*

/** Example from Rock the JVM
  * @see
  *   https://www.youtube.com/watch?v=PaogLRrYo64&list=PLmtsMNDRU0Bzu7NfhTiGK7iCYjcFAYlal
  */
object ZLayerExample extends ZIOAppDefault {

  private val gunther = User("gunther", "gunther@gunther.de")
  case class User(name: String, email: String)

  object UserEmailer {
    type UserEmailerEnv = UserEmailer.Service

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
      ZIO.environmentWithZIO(_.get.notify(user, message))
  }

  object UserDb {
    type UserDbEnv = UserDb.Service

    trait Service {
      def insert(user: User): Task[Unit]
    }

    val live: ULayer[UserDbEnv] = ZLayer.succeed(new Service {
      override def insert(user: User): Task[Unit] = Task {
        println(s"s[Database] insert into public.user values ('${user.email}')'")
      }
    })

    def insert(user: User): ZIO[UserDbEnv, Throwable, Unit] =
      ZIO.environmentWithZIO(_.get.insert(user))
  }

  // Horizontal composition
  val userBackendLayer: ZLayer[Any, Nothing, UserDbEnv & UserEmailerEnv] = UserDb.live ++ UserEmailer.live

//  override def run =
//    UserEmailer
//      .notify(gunther, "Willkommen")
//      .provideLayer(userBackendLayer)

  // Vertical composition
  object UserSubscription {
    type UserSubscriptionEnv = UserSubscription.Service
    class Service(notifier: UserEmailer.Service, userDb: UserDb.Service) {
      def subscribe(user: User): Task[User] =
        for {
          _ <- notifier.notify(gunther, s"Willkommen ${user.name}")
          _ <- userDb.insert(user)
        } yield user
    }

    val live: ZLayer[UserEmailerEnv & UserDbEnv, Nothing, UserSubscriptionEnv] =
      (new Service(_, _)).toLayer

    // front facing API
    def subscribe(user: User): ZIO[UserSubscriptionEnv, Throwable, User] =
      ZIO.environmentWithZIO(_.get.subscribe(user))
  }

  val userSubscriptionLayer: ZLayer[Any, Nothing, UserSubscriptionEnv] = userBackendLayer >>> UserSubscription.live

  override def run: ZIO[Any, Throwable, User] =
    UserSubscription
      .subscribe(gunther)
      .provideLayer(userSubscriptionLayer)
}
