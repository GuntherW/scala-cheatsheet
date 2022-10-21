package de.wittig.rockthejvm

import zhttp.http.{Http, Middleware, Request, Response}
import zio.{Console, ZIO}

object VerboseLoggingMiddleware {
  def log[R, E >: Exception]: Middleware[R, E, Request, Response, Request, Response] = new Middleware[R, E, Request, Response, Request, Response] {
    override def apply[R1 <: R, E1 >: E](http: Http[R1, E1, Request, Response]): Http[R1, E1, Request, Response] =
      http
        .contramapZIO[R1, E1, Request] { request => // Middleware, auf dem Request mit contramap
          for {
            _ <- Console.printLine(s"< [request] ${request.method} ${request.path} ${request.version}")
            _ <- ZIO.foreach(request.headers.toList)(header => Console.printLine(s"< [header] ${header._1}: ${header._2}"))
          } yield request
        }
        .mapZIO[R1, E1, Response](response => // Middleware, auf der Response mit map
          for {
            _ <- Console.printLine(s"> [status] ${response.status}")
            _ <- ZIO.foreach(response.headers.toList)(header => Console.printLine(s"> [header] ${header._1}: ${header._2}"))
            _ <- Console.printLine("-" * 50)
          } yield response
        )
  }
}
