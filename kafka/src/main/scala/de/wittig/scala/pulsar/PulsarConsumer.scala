package de.wittig.scala.pulsar

import scala.annotation.tailrec
import scala.util.{Failure, Success}

import com.sksamuel.pulsar4s.*
import com.sksamuel.pulsar4s.circe.*
import de.wittig.scala.pulsar.PulsarConf.*
import de.wittig.scala.pulsar.SensorDomain.SensorEvent
import io.circe.generic.auto.*
import org.apache.pulsar.client.api.{SubscriptionInitialPosition, SubscriptionType}

object PulsarConsumer extends App:

  private val pulsarClient   = PulsarClient(url)
  private val consumerConfig = ConsumerConfig(
    Subscription("sensor-event-subscription"),
    Seq(topic),
    consumerName = Some("sensor-event-consumer"),
    subscriptionInitialPosition = Some(SubscriptionInitialPosition.Earliest),
    subscriptionType = Some(SubscriptionType.Exclusive)
  )
  private val consumer       = pulsarClient.consumer[SensorEvent](consumerConfig)

  @tailrec
  private def receiveAll(totalMessageCount: Int = 0): Unit =
    consumer.receive match
      case Failure(exception) =>
        println(s"Failed to receive message ${exception.getMessage}")
      case Success(message)   =>
        println(s"Total messages : $totalMessageCount, Acked message ${message.messageId} - ${message.value}")
        consumer.acknowledge(message.messageId)
        receiveAll(totalMessageCount + 1)

  receiveAll()
