package de.wittig.ziokafka

import scala.concurrent.duration.DurationInt

import de.wittig.ziokafka.ConsumerGroup1Client1._settings
import zio.Console.printLine
import zio.{Duration, Schedule, ZIO, ZIOAppDefault, ZLayer}
import zio.kafka.consumer.ConsumerSettings
import zio.kafka.producer.*
import zio.*
import zio.Console.*
import zio.kafka.serde.*

object MyProducer extends ZIOAppDefault:

  private val managedProducer: ZIO[Scope, Throwable, Producer] = Producer.make(ProducerSettings(List("localhost:9092")))
  private val producer: ZLayer[Scope, Throwable, Producer]     = ZLayer.fromZIO(managedProducer)
  private def produce(key: String, value: String)              = Producer.produce("orders-by-user", key, value, Serde.string, Serde.string)

  private val program = for {
    key        <- Random.nextIntBetween(100, 1000).map(_.toString)
    value      <- Random.nextString(10)
    recordMeta <- produce(key, value).debug(s"[$key:$value]")
  } yield recordMeta

  def run = program
    .provideSomeLayer(producer)
    .repeat(Schedule.spaced(Duration.fromSeconds(2)))
