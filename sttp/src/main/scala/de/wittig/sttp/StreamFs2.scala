package de.wittig.sttp

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.*
import sttp.client4.httpclient.fs2.HttpClientFs2Backend

object StreamFs2 extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    HttpClientFs2Backend
      .resource[IO]()
      .use: backend =>
        val stream: Stream[IO, Byte] = Stream.emits("Hello, world".getBytes).repeatN(1000)
        basicRequest
          .post(uri"https://httpbin.org/post")
          .streamBody(Fs2Streams[IO])(stream)
          .response(asStreamAlways(Fs2Streams[IO])(_.chunks.map(_.size).compile.foldMonoid))
          .send(backend)
          .map(response => println(s"Bytes count:\n${response.body}"))
      .map(_ => ExitCode.Success)
