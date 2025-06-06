package de.wittig.scala.pulsar

import com.sksamuel.pulsar4s.*

import java.util.UUID
import scala.util.Random

object SensorDomain:

  case class SensorEvent(sensorId: String, status: String, startupTime: Long = startupTime, eventTime: Long, reading: Double)

  private val startupTime = System.currentTimeMillis
  private val sensorIds   = List.fill(10)(UUID.randomUUID.toString)
  private val offSensors  = sensorIds.toSet
  private val onSensors   = Set.empty[String]

  def generate(
      ids: List[String] = sensorIds,
      off: Set[String] = offSensors,
      on: Set[String] = onSensors
  ): Iterator[SensorEvent] =

    Thread.sleep(Random.nextInt(500) + 200)
    val index    = Random.nextInt(sensorIds.size)
    val sensorId = sensorIds(index)
    val reading  =
      if (off(sensorId))
        println(s"starting sensor $index")
        SensorEvent(sensorId, "Starting", startupTime, System.currentTimeMillis, 0.0)
      else
        val temp = BigDecimal(40 + Random.nextGaussian()).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
        SensorEvent(sensorId, "Running", startupTime, System.currentTimeMillis, temp)

    Iterator.single(reading) ++ generate(ids, off - sensorId, on + sensorId)
  end generate

object PulsarConf:
  val url: String  = "pulsar://localhost:6650"
  val topic: Topic = Topic("sensor-events")
