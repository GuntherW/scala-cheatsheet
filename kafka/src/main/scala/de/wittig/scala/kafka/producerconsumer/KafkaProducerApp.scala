package de.wittig.scala.kafka.producerconsumer

import java.time.{LocalDate, LocalDateTime}
import java.util.{Properties, UUID}

import scala.util.{Failure, Success, Try}

import de.wittig.scala.kafka.producerconsumer.Constants.*
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/** kafka-topics --bootstrap-server localhost:9092 --topic text_topic --create
  */

object KafkaProducerApp extends App:

  val props: Properties = new Properties()
  props.put("bootstrap.servers", bootstrapServer)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("acks", "all")

  val producer = new KafkaProducer[String, String](props)

  try
    for (i <- 0 to 15)
      val r        = new ProducerRecord[String, String](topic, i.toString, s"""{ "test": $i, "uuid": "${UUID.randomUUID().toString}" "ts": "${LocalDateTime.now}" """)
      val metadata = producer.send(r)
      println(s"sent (key=${r.key} value=${r.value}) meta(partition=${metadata.get.partition}, offset=${metadata.get.offset})")
  catch
    case e: Exception => e.printStackTrace()
  finally
    producer.close()