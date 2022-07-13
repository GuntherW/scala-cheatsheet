package de.wittig.scala.kafka.producerconsumer

import java.util.Properties

import scala.util.{Failure, Success, Try}

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/** kafka-topics --bootstrap-server localhost:9092 --topic text_topic --create
  */

object KafkaProducerApp extends App:

  val props: Properties = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("acks", "all")

  val producer = new KafkaProducer[String, String](props)
  val topic    = "text_topic"

  try
    for (i <- 0 to 15)
      val r        = new ProducerRecord[String, String](topic, i.toString, s"Test $i")
      val metadata = producer.send(r)
      println(s"sent (key=${r.key} value=${r.value}) meta(partition=${metadata.get.partition}, offset=${metadata.get.offset})")
  catch
    case e: Exception => e.printStackTrace()
  finally
    producer.close()
