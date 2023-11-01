package de.wittig.scala.kafka.producerconsumer

import java.time.Duration
import java.util.{Collections, Properties}
import java.util.regex.Pattern

import org.apache.kafka.clients.consumer.KafkaConsumer
import scala.jdk.CollectionConverters.*

import de.wittig.scala.kafka.producerconsumer.Constants.*

object KafkaConsumerApp extends App {

  private val props: Properties = new Properties()
  props.put("group.id", "test")
  props.put("bootstrap.servers", bootstrapServer)
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("enable.auto.commit", "true")
  props.put("auto.commit.interval.ms", "1000")

  private val consumer = new KafkaConsumer(props)
  private val topics   = List(topic)

  try
    consumer.subscribe(topics.asJava)
    while (true)
      val records = consumer.poll(Duration.ofMillis(100))
      records.asScala.zipWithIndex.foreach {
        case (record, index) => println(s"i:$index, Topic: ${record.topic},Key: ${record.key},Value: ${record.value}, Offset: ${record.offset}, Partition: ${record.partition}")
      }
  catch
    case e: Exception => e.printStackTrace()
  finally
    consumer.close()
}
