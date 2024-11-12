package de.wittig.scala.pulsar

import com.sksamuel.pulsar4s.*
import com.sksamuel.pulsar4s.circe.*
import de.wittig.scala.pulsar.PulsarConf.*
import de.wittig.scala.pulsar.SensorDomain.SensorEvent
import io.circe.generic.auto.*

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
