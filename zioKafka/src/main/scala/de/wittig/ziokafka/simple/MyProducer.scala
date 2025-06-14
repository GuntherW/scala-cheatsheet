package de.wittig.ziokafka.simple

import de.wittig.ziokafka.MyConstants.{bootstrapServer, topics}
import zio.*
import zio.kafka.producer.*
import zio.kafka.serde.*

/** // Löschen, falls Topic mit nur einer Partition
  *
  * kafka-topics --bootstrap-server localhost:9092 --topic zioKafka --delete
  *
  * // Anlegen des Topic mit zwei Partitionen
  *
  * kafka-topics --bootstrap-server localhost:9092 --topic zioKafka --create --partitions 2
  */
object MyProducer extends ZIOAppDefault:

  private val managedProducer: ZIO[Scope, Throwable, Producer] = Producer.make(ProducerSettings(List(bootstrapServer)))
  private val producer: ZLayer[Scope, Throwable, Producer]     = ZLayer.fromZIO(managedProducer)
  private val topic                                            = topics.split(",").head
  private def produce(key: String, value: String)              =
    for
      producer <- ZIO.service[Producer]
      meta     <- producer.produce(topic, key, value, Serde.string, Serde.string)
    yield meta

  private val program = for {
    key        <- Random.nextIntBetween(10000, 100000).map(_.toString)
    value      <- Random.nextString(10)
    recordMeta <- produce(key, value).debug(s"[$key:$value]")
  } yield recordMeta

  def run = program
    .provideSomeLayer(producer)
    .repeat(Schedule.spaced(Duration.fromSeconds(2)))
