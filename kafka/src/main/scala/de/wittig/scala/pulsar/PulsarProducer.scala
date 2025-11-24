package de.wittig.scala.pulsar

import com.sksamuel.pulsar4s.*
import com.sksamuel.pulsar4s.circe.*
import de.wittig.scala.pulsar.PulsarConf.*
import de.wittig.scala.pulsar.SensorDomain.SensorEvent
import io.circe.generic.auto.*

import scala.concurrent.ExecutionContext.Implicits.global

@main
def pulsarProducer(): Unit =

  val pulsarClient   = PulsarClient(url)
  val producerConfig = ProducerConfig(
    topic,
    producerName = Some("sensor-producer"),
    enableBatching = Some(true),
    blockIfQueueFull = Some(true)
  )
  val eventProducer  = pulsarClient.producer[SensorEvent](producerConfig)

  SensorDomain
    .generate()
    .take(100)
    .foreach { sensorEvent =>
      val message = DefaultProducerMessage(
        Some(sensorEvent.sensorId),
        sensorEvent,
        eventTime = Some(EventTime(sensorEvent.eventTime))
      )
      eventProducer.sendAsync(message)
    }
