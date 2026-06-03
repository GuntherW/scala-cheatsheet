package proteus.examples.greeter

import java.util.concurrent.TimeUnit
import io.grpc.ServerBuilder
import proteus.server.{DirectServerBackend, ServerService}

import scala.util.chaining.scalaUtilChainingOps

class GreeterServer(port: Int):

  val service = ServerService(using DirectServerBackend).rpc(sayHelloRpc, sayHello).build(greeterService)
  val server  = ServerBuilder.forPort(port).addService(service).build()

  def sayHello(request: HelloRequest): HelloReply =
    println(s"Server: SayHello(${request.name})")
    HelloReply(s"Hello, ${request.name}!")

  def start(): Unit =
    server.start()
    println(s"Server started on port $port")
    sys.addShutdownHook(stop()): Unit

  def stop(): Unit =
    server.shutdown().awaitTermination(5, TimeUnit.SECONDS): Unit
