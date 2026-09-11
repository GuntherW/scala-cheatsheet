import scala.concurrent.duration.*

import ox.{supervised, Ox}

import sage.*
import sage.backend.*

/** Beispielprojekt für die Bibliothek Sage (https://ghostdogpr.github.io/sage) mit dem Ox-Backend gegen einen lokalen Valkey-Server (siehe docker/docker-compose.yml, Service "valkey", Port 6379).
  *
  * Start des Servers: docker compose -f docker/docker-compose.yml up -d valkey
  *
  * Ausführen: scala-cli run cli/sage
  */
@main
def sageDemo(): Unit =
  supervised {
    val config = SageConfig(
      topology = Topology.Standalone(Endpoint("localhost", 6379))
    )

    val client = SageClient.scoped(config)

    stringsDemo(client).pprint("Strings")
    hashesDemo(client).pprint("Hashes")
    listsDemo(client).pprint("Lists")
    setsDemo(client).pprint("Sets")
    sortedSetsDemo(client).pprint("Sorted Sets")
    keysDemo(client).pprint("Keys & Expiry")
    customCodecDemo(client).pprint("Eigener Codec (User)")
    pipelineDemo(client).pprint("Pipeline")
    transactionDemo(client).pprint("Transaktion")
    pubSubDemo(client).pprint("Pub/Sub")
    streamsDemo(client).pprint("Streams")
    cachedReadDemo(client).pprint("Client-side caching")
  }

// Strings: set/get, incrBy
private def stringsDemo(client: SageClient)(using Ox): Unit =
  client.set("greeting", "hello")
  val greeting = client.get[String]("greeting")
  println(s"greeting=$greeting") // Some("hello")

  client.set("counter", 0)
  val counter = client.incrBy("counter", 10)
  println(s"counter=$counter") // 10

// Hashes: hSet/hGetAll
private def hashesDemo(client: SageClient)(using Ox): Unit =
  client.del("user:1")
  client.hSet("user:1", ("name", "Ada"), ("age", "36"))
  val profile = client.hGetAll[String, String]("user:1")
  println(s"profile=$profile") // Map("name" -> "Ada", "age" -> "36")

// Lists: rPush/lRange
private def listsDemo(client: SageClient)(using Ox): Unit =
  client.del("queue:jobs")
  client.rPush("queue:jobs", "job-1", "job-2", "job-3")
  val jobs = client.lRange[String]("queue:jobs", 0, -1)
  println(s"jobs=$jobs") // Vector("job-1", "job-2", "job-3")

// Sets: sAdd/sMembers
private def setsDemo(client: SageClient)(using Ox): Unit =
  client.del("tags:article-1")
  client.sAdd("tags:article-1", "scala", "sage", "valkey")
  val tags = client.sMembers[String]("tags:article-1")
  println(s"tags=$tags") // Set("scala", "sage", "valkey")

// Sorted sets: zAdd/zRange
private def sortedSetsDemo(client: SageClient)(using Ox): Unit =
  client.del("leaderboard")
  client.zAdd("leaderboard")(("ada", 100.0), ("grace", 95.0), ("linus", 120.0))
  val ranking = client.zRangeWithScores[String]("leaderboard", ZRange.ByRank(0, -1, rev = true))
  println(s"ranking=$ranking") // Vector(("linus", 120.0), ("ada", 100.0), ("grace", 95.0))

// Keys: exists/expire/ttl
private def keysDemo(client: SageClient)(using Ox): Unit =
  client.set("session:abc", "active")
  val existsBefore = client.exists("session:abc")
  client.expire("session:abc", 30.seconds)
  val ttl          = client.ttl("session:abc")
  println(s"exists=$existsBefore, ttl=$ttl") // exists=1, ttl=Expires(<= 30s)

private def customCodecDemo(client: SageClient)(using Ox): Unit =
  client.set("user:ada", User("Ada", 36))
  val ada = client.get[User]("user:ada")
  println(s"ada=$ada") // Some(User("Ada", 36))

// Pipeline: mehrere Commands in einem Roundtrip, typisiertes Tupel-Ergebnis
private def pipelineDemo(client: SageClient)(using Ox): Unit =
  client.set("pipe:a", "x")
  client.set("pipe:n", 10)
  val (a, n) = client.pipeline(
    (
      Commands.get[String, String]("pipe:a"),
      Commands.incrBy("pipe:n", 5)
    )
  )
  println(s"pipeline a=$a n=$n") // a=Some("x") n=15

// Transaktion: MULTI/EXEC mit optimistischem WATCH
private def transactionDemo(client: SageClient)(using Ox): Unit =
  client.set("tx:n", 1)
  val result = client.transaction { tx =>
    tx.watch("tx:n")
    val _ = tx.get[Int]("tx:n") // nur zu Demo-Zwecken gelesen, Wert wird nicht weiterverwendet
    tx.exec(
      (Commands.incr("tx:n"), Commands.incrBy("tx:n", 4))
    )
  }
  println(s"transaction result=$result") // Some((2, 6))

// Pub/Sub: subscribeScoped liefert einen Ox-Flow, publish sendet Nachrichten
private def pubSubDemo(client: SageClient)(using Ox): Unit =
  val news     = client.subscribeScoped[String]("news")
  (1 to 3).foreach(i => client.publish("news", s"item-$i"))
  val messages = news.take(3).runToList()
  println(s"messages=${messages.map(_.payload)}") // List("item-1", "item-2", "item-3")

// Streams: xAdd/xLen/xRange und Consumer-Gruppen
private def streamsDemo(client: SageClient)(using Ox): Unit =
  client.del("stream:orders")
  client.xAdd("stream:orders")(("item", "book"), ("qty", "2"))
  client.xAdd("stream:orders")(("item", "pen"), ("qty", "5"))
  val len     = client.xLen("stream:orders")
  val entries = client.xRange[String, String]("stream:orders")
  println(s"len=$len entries=${entries.map(_.fields)}")

  client.xGroupCreate("stream:orders", "workers", id = GroupStartId.At(StreamId.Zero))
  val batches = client.xReadGroup[String, String]("workers", "w1")(("stream:orders", GroupReadId.New))()
  val ids     = batches.flatMap { case (_, entries) => entries }.map(_.id)
  ids match
    case head +: tail => client.xAck("stream:orders", "workers")(head, tail*)
    case _            => println("keine neuen Stream-Einträge zum Acken")
  println(s"acked ids=$ids")

// Client-side caching: erster Read holt vom Server, zweiter kommt aus dem lokalen Cache
private def cachedReadDemo(client: SageClient)(using Ox): Unit =
  client.set("cached:key", "v1")
  val v1 = client.cached(Commands.get[String, String]("cached:key"), 1.minute)
  val v2 = client.cached(Commands.get[String, String]("cached:key"), 1.minute)
  println(s"cached v1=$v1 v2=$v2") // beide Some("v1"), v2 aus dem lokalen Cache

extension (body: => Unit)
  private def pprint(title: String): Unit =
    println(s"\n--- $title ---")
    body

// Eigener ValueCodec für einen eigenen Typ. Sage hat kein `derives ValueCodec` (bewusst kein
// Magnolia/Mirror im Core), daher wird der Codec mit `emap`/`imap` von Hand aus `ValueCodec[String]` abgeleitet.
final case class User(name: String, age: Int)

private object User:
  private val format = "User(name|age)"

  given ValueCodec[User] = ValueCodec[String].emap {
    case raw @ s"$name|$age" => age.toIntOption.map(User(name, _)).toRight(SageException.DecodeError(format, raw))
    case raw                 => Left(SageException.DecodeError(format, raw))
  }(user => s"${user.name}|${user.age}")
