package de.wittig.proto.core

import de.wittig.proto.orders.*
import cats.effect.*
import io.grpc.*
import fs2.Stream
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import fs2.grpc.syntax.all.*

//https://blog.rockthejvm.com/grpc-in-scala-with-fs2-scalapb/

// Business logic
class OrderService extends OrderFs2Grpc[IO, Metadata]:
  override def sendOrderStream(request: Stream[IO, OrderRequest], ctx: Metadata): Stream[IO, OrderReply] =
    request.map { orderReq =>
      OrderReply(
        orderReq.orderid,
        orderReq.items,
        orderReq.items.map(i => i.qty * i.amount).sum
      )
    }

object OrderServer extends IOApp.Simple {

  private val grpcServer: Resource[IO, Server] = OrderFs2Grpc
    .bindServiceResource[IO](new OrderService)
    .flatMap { service =>
      NettyServerBuilder
        .forPort(9999)
        .addService(service)
        .resource[IO]
    }

  override def run: IO[Unit] =
    grpcServer.use { server =>
      IO.println("Server started") *> IO(server.start()) *> IO.never
    }
}

object OrderClient extends IOApp.Simple:
  private val resource: Resource[IO, OrderFs2Grpc[IO, Metadata]] =
    NettyChannelBuilder
      .forAddress("127.0.0.1", 9999)
      .usePlaintext()
      .resource[IO]
      .flatMap(ch => OrderFs2Grpc.stubResource[IO](ch))

  override def run: IO[Unit] =
    resource.use { stub =>
      val orders = OrderRequest(1, List(Item("1", 2, 1.99), Item("2", 3, 4.99)))
      val stream = Stream.emits(List(orders))

      stub
        .sendOrderStream(stream, new Metadata)
        .compile
        .toList
        .flatMap(IO.println)
    }
end OrderClient
