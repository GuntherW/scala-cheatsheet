package de.wittig.scala.pulsar

import de.wittig.scala.pulsar.SensorDomain.{generate, SensorEvent}
import com.sksamuel.pulsar4s.*
import com.sksamuel.pulsar4s.circe.*
import io.circe.generic.auto.*
import PulsarConf.*

object PulsarProducer extends App:

  import scala.concurrent.ExecutionContext.Implicits.global

  private val pulsarClient   = PulsarClient(url)
  private val producerConfig = ProducerConfig(
    topic,
    producerName = Some("sensor-producer"),
    enableBatching = Some(true),
    blockIfQueueFull = Some(true)
  )
  private val eventProducer  = pulsarClient.producer[SensorEvent](producerConfig)

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
