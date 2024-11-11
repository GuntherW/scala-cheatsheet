package de.wittig.akka.discovery

import scala.collection.immutable.SortedMap
import scala.concurrent.duration.*
import scala.util.Random

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

/** Discovery mit dem Pattern "Receptionist"
  * @see
  *   https://blog.rockthejvm.com/akka-actor-discovery/
  */

object AkkaActorDiscovery extends App {

  case class SensorReading(id: String, value: Int)

  object DataAggregator {
    val serviceKey: ServiceKey[SensorReading] = ServiceKey[SensorReading]("dataAggregator")

    def apply(): Behavior[SensorReading] = active(SortedMap.empty)

    def active(latestReadings: SortedMap[String, Int]): Behavior[SensorReading] = Behaviors.receive { (context, reading) =>
      val SensorReading(id, value) = reading
      val newReadings              = latestReadings + (id -> value)
      val newReadingsForLog        = newReadings.map {
        case (`id`, value) => s"${Console.GREEN}$id->$value${Console.RESET}"
        case (key, value)  => s"$key->$value"
      }.mkString(",")
      context.log.info(s"[${context.self.path.name}] Latest readings: $newReadingsForLog")
      active(newReadings)
    }
  }

  trait SensorCommand
  case object SensorHeartbeat                                           extends SensorCommand
  case class ChangeDataAggregator(agg: Option[ActorRef[SensorReading]]) extends SensorCommand

  object Sensor {
    def apply(id: String): Behavior[SensorCommand] = Behaviors.setup { context =>
      // use a message adapter to turn a receptionist listing into a SensorCoimmand
      val receptionistSubscriber: ActorRef[Receptionist.Listing] = context.messageAdapter {
        case DataAggregator.serviceKey.Listing(set) => ChangeDataAggregator(set.headOption)
      }

      // subscribe to the receptionist, using servive key
      context.system.receptionist ! Receptionist.subscribe(DataAggregator.serviceKey, receptionistSubscriber)
      activeSensor(id, None)
    }

    def activeSensor(id: String, aggregator: Option[ActorRef[SensorReading]]): Behavior[SensorCommand] = Behaviors.receiveMessage {
      case SensorHeartbeat              =>
        aggregator.foreach(_ ! SensorReading(id, Random.nextInt(100)))
        Behaviors.same
      case ChangeDataAggregator(newAgg) =>
        activeSensor(id, newAgg)
    }

    def controller(): Behavior[NotUsed] = Behaviors.setup { context =>
      val sensors = (1 to 5).map(i => context.spawn(Sensor(s"sensor_$i"), s"sensor_$i"))
      val logger  = context.log

      // send heartbeats every second
      import context.executionContext
      context.system.scheduler.scheduleAtFixedRate(1.second, 1.second) { () =>
        logger.info("Heartbeat")
        sensors.foreach(_ ! SensorHeartbeat)
      }

      Behaviors.empty
    }
  }

  val guardian: Behavior[NotUsed] = Behaviors.setup { context =>
    // controller
    val sensorController = context.spawn(Sensor.controller(), "controller")

    val dataAgg1 = context.spawn(DataAggregator(), "data_agg_1")
    context.system.receptionist ! Receptionist.register(DataAggregator.serviceKey, dataAgg1) // publish dataAgg1 is available

    // change the dataAggregator after 10s
    Thread.sleep(10000)
    context.log.info("[guardian] Changing data aggregator")
    context.system.receptionist ! Receptionist.deregister(DataAggregator.serviceKey, dataAgg1)
    val dataAgg2 = context.spawn(DataAggregator(), "data_agg_2")
    context.system.receptionist ! Receptionist.register(DataAggregator.serviceKey, dataAgg2) // publish dataAgg2 is availabe

    Behaviors.empty
  }

  val system = ActorSystem(guardian, "ActorDiscovery")
  import system.executionContext
  system.scheduler.scheduleOnce(20.seconds, () => system.terminate())
}
