package de.wittig.tapirr.ox

import ox.*
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerOptions}

@main
def run(): Unit =

  val serverOptions = NettySyncServerOptions
    .customiseInterceptors
    .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
    .options

  supervised {
    val binding = useInScope(
      NettySyncServer(serverOptions)
        .port(8086)
        .addEndpoints(Endpoints.all)
        .start()
    )(_.stop())
    println(s"Go to http://localhost:${binding.port}/docs to open SwaggerUI. ")
    never
  }

//  NettySyncServer(serverOptions)
//    .port(8087)
//    .addEndpoints(Endpoints.all)
//    .tap(_ => println("Go to http://localhost:8087/docs to open SwaggerUI."))
//    .startAndWait()
