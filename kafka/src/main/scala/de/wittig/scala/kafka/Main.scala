package de.wittig.scala.kafka
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.{GlobalKTable, JoinWindows, TimeWindows, Windowed}
import org.apache.kafka.streams.scala.ImplicitConversions.*
import org.apache.kafka.streams.scala.*
import org.apache.kafka.streams.scala.kstream.{KGroupedStream, KStream, KTable}
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.scala.serialization.Serdes.*
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig, Topology}

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Properties
import scala.concurrent.duration.*
import Domain.*
import Topics.*
import scala.util.Random

/** https://www.youtube.com/watch?v=MYTFPTtOoLs&t=156s https://blog.rockthejvm.com/kafka-streams/
  */
object KafkaStreams extends App:

  implicit def serde[A >: Null: Decoder: Encoder]: Serde[A] =
    val serializer   = (a: A) => a.asJson.noSpaces.getBytes()
    val deserializer = (bs: Array[Byte]) => decode[A](new String(bs)).toOption
    Serdes.fromFn[A](serializer, deserializer)

  val builder = new StreamsBuilder()

  // KTable - (distributed)
  val userProfilesTable: KTable[UserId, Profile] = builder.table[UserId, Profile](DiscountProfilesByUserTable.value)

  // GlobalKTable - (copied to all the nodes)
  val discountProfilesGTable: GlobalKTable[Profile, Discount] = builder.globalTable[Profile, Discount](DiscountsTable.value)

  // KStream
  val userOrdersStream: KStream[UserId, Order] = builder.stream[UserId, Order](OrdersByUserTopic.value)
  val paymentsStream                           = builder.stream[OrderId, Payment](PaymentsTopic.value)

  // KStream transformations
  val expensiveOrders = userOrdersStream.filter((userId, order) => order.amount > 1000)
  val listOfProducts  = userOrdersStream.mapValues(_.products)
  val productStream   = userOrdersStream.flatMapValues(_.products)

  // join
  val ordersWithUserProfiles = userOrdersStream.join(userProfilesTable)((order, profile) => (order, profile))
  ordersWithUserProfiles.to(DebugOrdersWithUserProfilesTopic.value)
  val discountedOrdersStream = ordersWithUserProfiles.join(discountProfilesGTable)(
    { case (userId, (order, profile)) => profile }, // key of the join, picked of the "left" stream
    { case ((order, profile), discount) => order.copy(amount = order.amount - discount.amount) }
  )
  discountedOrdersStream.to(DebugOrdersWithUserProfilesDiscountedTopic.value)

  // pick another identifier
  val ordersStream = discountedOrdersStream.selectKey((userId, order) => order.orderId)
  ordersStream.to(DebugOrdersTopic.value)

  // Join Window
  val joinWindow        = JoinWindows.of(Duration.of(5, ChronoUnit.MINUTES))
  val joinOrderPayments = (order: Order, payments: Payment) => if payments.status == "PAID" then Option(order) else Option.empty[Order]
  val ordersPaid        = ordersStream.join[Payment, Option[Order]](paymentsStream)(joinOrderPayments, joinWindow)
    .flatMapValues(maybeOrder => maybeOrder.toList)

  // sink
  ordersPaid.to(PaidOrdersTopic.value)

  val topology = builder.build()
  println(topology.describe) // prints topology to console

  val props = new Properties
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "orders-application")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.stringSerde.getClass)

  val application = new KafkaStreams(topology, props)
  application.start()
//  Thread.sleep(120000)
//  application.close()

object Domain:
  type UserId  = String
  type Profile = String
  type Product = String
  type OrderId = String

  case class Order(orderId: OrderId, user: UserId, products: List[Product], amount: Double)
  case class Discount(profile: Profile, amount: Double) // in percentage points
  case class Payment(orderId: OrderId, status: String)

enum Topics(val value: String, val compact: Boolean = false):
  case DiscountProfilesByUserTable                extends Topics("discount-profiles-by-user", true)
  case DiscountsTable                             extends Topics("discounts", true)
  case OrdersByUserTopic                          extends Topics("orders-by-user")
  case PaymentsTopic                              extends Topics("payments")
  case PaidOrdersTopic                            extends Topics("paid-orders")
  case DebugOrdersWithUserProfilesTopic           extends Topics("orders-with-user-profiles")
  case DebugOrdersWithUserProfilesDiscountedTopic extends Topics("orders-with-user-profiles-discounted")
  case DebugOrdersTopic                           extends Topics("orders")

/** login first with `docker exec -it kafka bash` */
@main def createTopics(): Unit =
  Topics.values.foreach { topic =>
    val create  = s"kafka-topics --bootstrap-server localhost:9092 --topic ${topic.value} --create"
    val compact = if (topic.compact) """ --config "cleanup.policy=compact"""" else ""
    println(create + compact)
  }

// In separaten Konsolen füllen:

//kafka-console-producer --topic discounts --broker-list localhost:9092 --property parse.key=true --property key.separator=,
//<Hit Enter>
//profile1,{"profile":"profile1","amount":0.5}
//profile2,{"profile":"profile2","amount":0.25}
//profile3,{"profile":"profile3","amount":0.15}

//kafka-console-producer --topic discount-profiles-by-user --broker-list localhost:9092 --property parse.key=true --property key.separator=,
//<Hit Enter>
//Daniel,profile1
//Riccardo,profile2

//kafka-console-producer --topic orders-by-user --broker-list localhost:9092 --property parse.key=true --property key.separator=,
//<Hit Enter>
//Daniel,{"orderId":"order1","user":"Daniel","products":[ "iPhone 13","MacBook Pro 15"],"amount":4000/.0 }
//Riccardo,{"orderId":"order2","user":"Riccardo","products":["iPhone 11"],"amount":800.0}
//Riccardo,{"orderId":"order3","user":"Riccardo","products":["iPhone 10"],"amount":800.0}

//kafka-console-producer --topic payments --broker-list localhost:9092 --property parse.key=true --property key.separator=,
//<Hit Enter>
//order1,{"orderId":"order1","status":"PAID"}
//order2,{"orderId":"order2","status":"PENDING"}

//kafka-console-consumer --bootstrap-server localhost:9092 --topic orders-with-user-profiles --from-beginning
//kafka-console-consumer --bootstrap-server localhost:9092 --topic orders-with-user-profiles-discounted --from-beginning
//kafka-console-consumer --bootstrap-server localhost:9092 --topic orders --from-beginning
//kafka-console-consumer --bootstrap-server localhost:9092 --topic paid-orders --from-beginning
