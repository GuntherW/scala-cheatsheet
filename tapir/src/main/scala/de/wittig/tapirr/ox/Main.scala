package de.wittig.tapirr.ox

import ox.*
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerOptions}

@main def run(): Unit =

  val serverOptions = NettySyncServerOptions
    .customiseInterceptors
    .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
    .options

  val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8086)

  supervised {
    val binding = useInScope(NettySyncServer(serverOptions).port(port).addEndpoints(Endpoints.all).start())(_.stop())
    println(s"Go to http://localhost:${binding.port}/docs to open SwaggerUI. ")
    never
  }
