package de.wittig.zio.stream

import java.io.{IOException, InputStream}

import zio.stream.{ZPipeline, ZSink, ZStream}
import zio.{Chunk, ZIO, ZIOAppDefault}

object ZioStream extends ZIOAppDefault:

  val aSuccess                            = ZIO.succeed(42)
  val aStream: ZStream[Any, Nothing, Int] = ZStream.fromIterable(1 to 10)
  val intStream                           = ZStream(1, 2, 3, 4, 5, 6)
  val stringStream                        = intStream.map(_.toString)

  val sum: ZSink[Any, Nothing, Int, Nothing, Int]                                    = ZSink.sum[Int]
  val take5: ZSink[Any, Nothing, Int, Int, Chunk[Int]]                               = ZSink.take(5)
  val take5Map: ZSink[Any, Nothing, Int, Int, Chunk[String]]                         = take5.map(chunk => chunk.map(_.toString))
  val take5Leftovers: ZSink[Any, Nothing, Int, Nothing, (Chunk[String], Chunk[Int])] = take5Map.collectLeftover
  val take5Ignore: ZSink[Any, Nothing, Int, Nothing, Chunk[Int]]                     = take5.ignoreLeftover
  val take5Strings: ZSink[Any, Nothing, String, Int, Chunk[Int]]                     = take5.contramap(_.toInt)

  // Transformer
  val businessLogic: ZPipeline[Any, Nothing, String, Int] = ZPipeline.map(_.toInt)
  val filterLogic: ZPipeline[Any, Nothing, Int, Int]      = ZPipeline.filter(_ % 2 == 0)
  val appLogic: ZPipeline[Any, Nothing, String, Int]      = businessLogic >>> filterLogic

  // Failures
  val failStream: ZStream[Any, String, Int] = ZStream(1, 2) ++ ZStream.fail("Something bad") ++ ZStream(4, 5)

  class FakeInputStream[T <: Throwable](limit: Int, failAt: Int, failWith: => T) extends InputStream {
    val data: Array[Byte]    = "0123456789".getBytes
    var counter: Int         = 0
    var index: Int           = 0
    override def read(): Int =
      if (counter == limit) -1
      else if (counter == failAt) throw failWith
      else {
        val result = data(index)
        index = (index + 1) % data.length
        counter += 1
        result
      }
  }

  def streamMayFail(limit: Int, failAt: Int): ZStream[Any, IOException, String] =
    ZStream
      .fromInputStream(new FakeInputStream(limit, failAt, new IOException("somethin bad")), chunkSize = 1)
      .map(byte => new String(Array(byte)))

  val sink: ZSink[Any, Nothing, String, Nothing, String] =
    ZSink.collectAll[String].map(chunk => chunk.mkString("-"))

  override def run = for {
    _ <- streamMayFail(12, 99).run(sink).debug
    _ <- intStream.run(sum).debug
    _ <- stringStream.via(businessLogic).run(sum).debug
    _ <- stringStream.via(appLogic).run(sum).debug
  } yield ()
