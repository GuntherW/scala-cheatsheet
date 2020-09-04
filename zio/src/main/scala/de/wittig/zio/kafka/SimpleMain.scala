package de.wittig.zio.kafka
import zio.{App, ExitCode, URIO}

import zio._, zio.duration._
import zio.kafka.consumer._
import zio._
import zio.console._
import zio.kafka.consumer._
import zio.kafka.serde._

object SimpleMain extends App {

  val settings: ConsumerSettings =
    ConsumerSettings(List("localhost:9092"))
      .withGroupId("group")
      .withClientId("client")
      .withCloseTimeout(5.seconds)

  val subscription = Subscription.topics(Topic.quickStart)

  override def run(args: List[String]) = {
    val con = Consumer.consumeWith(settings, subscription, Serde.string, Serde.string) {
      case (key, value) => putStrLn(s"############################### Received message $key: $value")
    }
    con.exitCode
  }
}
