package de.wittig.ziokafka

import de.wittig.ziokafka.ConsumerGroup1Client1.consumerIO
import zio.*
import zio.Console.*
import zio.kafka.consumer.*
import zio.kafka.serde.*

object ConsumerGroup1Client1 extends ZIOAppDefault with KafkaCommon:
  def settings: ConsumerSettings =
    _settings
      .withGroupId("g1")
      .withClientId("g1c1")

object ConsumerGroup1Client2 extends ZIOAppDefault with KafkaCommon:
  def settings: ConsumerSettings =
    _settings
      .withGroupId("g1")
      .withClientId("g1c2")

object ConsumerGroup2Client1 extends ZIOAppDefault with KafkaCommon:
  def settings: ConsumerSettings =
    _settings
      .withGroupId("g2")
      .withClientId("g2c1")

trait KafkaCommon {
  ZIOAppDefault =>

  val _settings: ConsumerSettings             = ConsumerSettings(List("localhost:9092")).withCloseTimeout(30.seconds)
  def settings: ConsumerSettings
  def topics                                  = "orders-by-user"
  private lazy val list                       = topics.split(",")
  private lazy val subscription: Subscription = Subscription.topics(list.head, list.tail*)
  def consumerIO: RIO[Any, Unit]              = Consumer.consumeWith(settings, subscription, Serde.string, Serde.string) {
    case (key, value) => ZIO.succeed(println(s"Received record: $key: $value"))
  }

  def run: RIO[Any, Unit] = consumerIO
}
