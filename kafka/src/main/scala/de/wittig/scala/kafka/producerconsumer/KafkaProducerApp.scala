package de.wittig.scala.kafka.producerconsumer

import java.time.LocalDateTime
import java.util.{Properties, UUID}

import de.wittig.scala.kafka.producerconsumer.Constants.*
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/** kafka-topics --bootstrap-server localhost:9092 --topic text_topic --delete
  *
  * kafka-topics --bootstrap-server localhost:9092 --topic text_topic --create
  *
  * kafka-topics --bootstrap-server localhost:9092 --topic text_topic --create --partitions 2
  *
  * Änderung der Partitionen zur Laufzeit:
  *
  * kafka-topics --bootstrap-server localhost:9092 --alter --topic text_topic --partitions 3
  */

object KafkaProducerApp extends App:

  private val props: Properties = new Properties()
  props.put("bootstrap.servers", bootstrapServer)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("acks", "all")

  private val producer = new KafkaProducer[String, String](props)

  try
    for (i <- 0 to 15)
      val r1       = new ProducerRecord[String, String](topic, i.toString, s"""{ "test": $i, "uuid": "${UUID.randomUUID().toString}" "ts": "${LocalDateTime.now}" """)
      val metadata = producer.send(r1)
      println(s"sent (key=${r1.key} value=${r1.value}) meta(partition=${metadata.get.partition}, offset=${metadata.get.offset()})")
  catch
    case e: Exception => e.printStackTrace()
  finally
    producer.close()
