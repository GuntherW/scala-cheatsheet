package de.wittig.pekko.hallowelt

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.Duration
import scala.concurrent.Await

final case class Greet(name: String, replyTo: ActorRef[Greeted])
final case class Greeted(message: String)

val greeter: Behavior[Greet] = Behaviors.receiveMessage {
  case Greet(name, replyTo) =>
    replyTo ! Greeted(s"Hallo, $name!")
    Behaviors.same
}

@main def helloPekko(): Unit =

  val root: Behavior[Greeted] = Behaviors.setup { ctx =>
    val greeterRef = ctx.spawn(greeter, "greeter")
    greeterRef ! Greet("Welt", ctx.self)
    Behaviors.receiveMessage {
      case Greeted(message) =>
        println(message)
        Behaviors.stopped
    }
  }

  val system = ActorSystem(root, "hello-pekko")
  Await.result(system.whenTerminated, Duration.Inf)
