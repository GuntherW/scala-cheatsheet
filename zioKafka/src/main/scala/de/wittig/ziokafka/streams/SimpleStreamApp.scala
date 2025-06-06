package de.wittig.ziokafka.streams

import de.wittig.ziokafka.MyConstants
import zio.*
import zio.kafka.consumer.*
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.*
import zio.stream.ZStream

object SimpleStreamApp extends ZIOAppDefault {

  val producer: ZStream[Producer, Throwable, Nothing] =
    ZStream
      .repeatZIO(Random.nextIntBetween(0, Int.MaxValue))
      .schedule(Schedule.fixed(1.second))
      .mapZIO { random =>
        ZIO.serviceWithZIO[Producer] {
          _.produce(
            topic = MyConstants.topics,
            key = (random % 4).toLong,
            value = random.toString,
            keySerializer = Serde.long,
            valueSerializer = Serde.string
          )
        }
      }
      .drain

  val consumer: ZStream[Consumer, Throwable, Nothing] =
    ZStream.serviceWithStream[Consumer](consumer =>
      consumer
        .plainStream(Subscription.topics(MyConstants.topics), Serde.long, Serde.string)
        .tap(r => Console.printLine(s"${r.key} - ${r.value} - ${r.partition}"))
        .map(_.offset)
        .aggregateAsync(Consumer.offsetBatches)
        .mapZIO(_.commit)
        .drain
    )

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
