package de.wittig.ziokafka.streams

import de.wittig.ziokafka.MyConstants
import zio.*
import zio.kafka.consumer.*
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.*
import zio.stream.ZStream

object BatchedStreamApp extends ZIOAppDefault {

  val producer: ZStream[Producer, Throwable, Nothing] =
    ZStream
      .repeatZIO(Random.nextIntBetween(0, Int.MaxValue))
      .schedule(Schedule.fixed(1.second))
      .tap(r => Console.printLine(s"Producing: $r"))
      .mapZIO { random =>
        Producer.produce[Any, Long, String](
          topic = MyConstants.topics,
          key = random % 4,
          value = random.toString,
          keySerializer = Serde.long,
          valueSerializer = Serde.string
        )
      }
      .drain

  val consumer: ZStream[Consumer, Throwable, Nothing] =
    Consumer
      .plainStream(Subscription.topics(MyConstants.topics), Serde.long, Serde.string)
      .tap(r => Console.printLine(s"Consuming: ${r.key} - ${r.value} - ${r.partition}"))
      .groupedWithin(5, 6.seconds)
      .mapZIO { batch =>
        processBatch(batch) *>
          batch.map(_.offset)
            .foldLeft(OffsetBatch.empty)(_ add _)
            .commit
      }
      .drain

  def processBatch(value: Chunk[CommittableRecord[Long, String]]) =
    ZIO.foreach(value)(r => Console.printLine(s"Consuming batched: ${r.key} - ${r.value} - ${r.partition}"))

  def producerLayer =
    ZLayer.scoped(
      Producer.make(settings = ProducerSettings(List(MyConstants.bootstrapServer)))
    )

  def consumerLayer =
    ZLayer.scoped(
      Consumer.make(ConsumerSettings(List(MyConstants.bootstrapServer)).withGroupId("group"))
    )

  override def run =
    producer.merge(consumer)
      .runDrain
      .provide(producerLayer, consumerLayer)
}
