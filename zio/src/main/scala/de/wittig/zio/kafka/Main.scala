package de.wittig.zio.kafka

import de.wittig.zio.Main.myAppLogic
import org.apache.kafka.clients.producer.ProducerRecord
import zio.kafka.consumer.{Consumer, ConsumerSettings, OffsetBatch, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.{Chunk, ZLayer, App}

object Main extends App {

  val consumerSettings: ConsumerSettings = ConsumerSettings(List("localhost:9092")).withGroupId("group")
  val producerSettings: ProducerSettings = ProducerSettings(List("localhost:9092"))

  val consumerAndProducer =
    ZLayer.fromManaged(Consumer.make(consumerSettings)) ++
      ZLayer.fromManaged(Producer.make(producerSettings, Serde.int, Serde.string))

  val consumeProduceStream = Consumer
    .subscribeAnd(Subscription.topics("quickstart-events"))
    .plainStream(Serde.int, Serde.long)
    .map { record =>
      val key: Int         = record.record.key()
      val value: Long      = record.record.value()
      val newValue: String = value.toString
      println("++++")
      println(newValue)
      val producerRecord: ProducerRecord[Int, String] = new ProducerRecord("my-output-topic", key, newValue)
      (producerRecord, record.offset)
    }
    .mapChunksM { chunk =>
      val records     = chunk.map(_._1)
      val offsetBatch = OffsetBatch(chunk.map(_._2).toSeq)

      Producer.produceChunk[Any, Int, String](records) *> offsetBatch.commit.as(Chunk(()))
    }
    .runDrain
    .provideSomeLayer(consumerAndProducer)

  def run(args: List[String]) =
    consumeProduceStream.exitCode
}
