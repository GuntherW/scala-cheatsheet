package de.wittig.ziokafka

import de.wittig.ziokafka.MyConstants.bootstrapServer
import zio.*
import zio.kafka.consumer.*
import zio.kafka.serde.*

object ConsumerGroup1Client1 extends ZIOAppDefault, KafkaCommon:
  def settings: ConsumerSettings =
    _settings
      .withGroupId("g1")
      .withClientId("g1c1")

object ConsumerGroup1Client2 extends ZIOAppDefault, KafkaCommon:
  def settings: ConsumerSettings =
    _settings
      .withGroupId("g1")
      .withClientId("g1c2")

object ConsumerGroup2Client1 extends ZIOAppDefault, KafkaCommon:
  def settings: ConsumerSettings =
    _settings
      .withGroupId("g2")
      .withClientId("g2c1")

trait KafkaCommon {
  ZIOAppDefault =>

  val _settings: ConsumerSettings             = ConsumerSettings(List(bootstrapServer)).withCloseTimeout(30.seconds)
  def settings: ConsumerSettings
  def topics                                  = MyConstants.topics
  private lazy val list                       = topics.split(",")
  private lazy val subscription: Subscription = Subscription.topics(list.head, list.tail*)
  def consumerIO: RIO[Any, Unit]              = Consumer.consumeWith(settings, subscription, Serde.string, Serde.string) {
    consumerRecord => ZIO.succeed(println(s"[${consumerRecord.partition}] record: ${consumerRecord.key}: ${consumerRecord.value}"))
  }

  def run: RIO[Any, Unit] = consumerIO
}
