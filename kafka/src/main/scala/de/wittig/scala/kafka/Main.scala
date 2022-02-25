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
    val deserializer = (bytes: Array[Byte]) => {
      val string = new String(bytes)
      decode[A](string).toOption
    }
    Serdes.fromFn[A](serializer, deserializer)

  // topology
  val builder = new StreamsBuilder()

  // KStream
  val userOrdersStream: KStream[UserId, Order] = builder.stream[UserId, Order](OrdersByUserTopic)

  // KTable - (distributed)
  val userProfilesTable: KTable[UserId, Profile] = builder.table[UserId, Profile](DiscountProfilesByUserTopic)

  // GlobalKTable - (copied to all the nodes)
  val discountProfilesGTable: GlobalKTable[Profile, Discount] = builder.globalTable[Profile, Discount](DiscountsTopic)

  // KStream transformations
  val expensiveOrders = userOrdersStream.filter { (userId, order) => order.amount > 1000 }
  val listOfProducts  = userOrdersStream.mapValues(_.products)
  val productStream   = userOrdersStream.flatMapValues(_.products)

  // join
  val ordersWithUserProfiles = userOrdersStream.join(userProfilesTable) { (order, profile) => (order, profile) }
  val discountedOrdersStream = ordersWithUserProfiles.join(discountProfilesGTable)(
    { case (userId, (order, profile)) => profile },
    { case ((order, profile), discount) => order.copy(amount = order.amount - discount.amount) }
  )

  // pick another identifier
  val ordersStream   = discountedOrdersStream.selectKey((userId, order) => order.orderId)
  val paymentsStream = builder.stream[OrderId, Payment](PaymentsTopic)

  // Join Window
  val joinWindow        = JoinWindows.of(Duration.of(5, ChronoUnit.MINUTES))
  val joinOrderPayments = (order: Order, payments: Payment) => if payments.status == "PAID" then Option(order) else Option.empty[Order]
  val ordersPaid        = ordersStream.join(paymentsStream)(joinOrderPayments, joinWindow)
    .flatMapValues(maybeOrder => maybeOrder.toIterable)

  // sink
  ordersPaid.to(PaidOrdersTopic)

  val topology = builder.build()
  println(topology.describe) // prints topology to console

  val props = new Properties
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "orders-application")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.stringSerde.getClass)

  val application = new KafkaStreams(topology, props)
  application.start

object Domain:
  type UserId  = String
  type Profile = String
  type Product = String
  type OrderId = String

  case class Order(orderId: OrderId, user: UserId, products: List[Product], amount: Double)
  case class Discount(profile: Profile, amount: Double) // in percentage points
  case class Payment(orderId: OrderId, status: String)

object Topics:
  final val OrdersByUserTopic           = "orders-by-user"
  final val DiscountProfilesByUserTopic = "discount-profiles-by-user"
  final val DiscountsTopic              = "discounts"
  final val OrdersTopic                 = "orders"
  final val PaymentsTopic               = "payments"
  final val PaidOrdersTopic             = "paid-orders"
