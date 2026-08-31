import chimp.client.*
import chimp.client.transport.ClientHttpTransport
import chimp.protocol.*
import chimp.server.StreamingMcpServer
import chimp.server.ox.OxServerHttpTransport
import munit.Fixture
import ox.{supervised, useInScope}
import sttp.client4.{DefaultSyncBackend, SyncBackend}
import sttp.model.Uri.UriContext
import sttp.shared.Identity
import sttp.tapir.server.netty.sync.NettySyncServer

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Try

class McpServerFixture(tools: StreamingMcpServer[Identity] => StreamingMcpServer[Identity])
    extends Fixture[McpClient[Identity]]("McpServer"):

  private var client: McpClient[Identity] = null
  private var backend: SyncBackend        = null
  private var serverShutdown: () => Unit  = () => ()

  def apply(): McpClient[Identity] = client

  override def beforeAll(): Unit =
    val ready    = CountDownLatch(1)
    val shutdown = CountDownLatch(1)
    val portRef  = new AtomicInteger(0)

    Thread.ofVirtual().start: () =>
      supervised:
        val endpoint = OxServerHttpTransport(List("mcp")).serve:
          tools(StreamingMcpServer[Identity]())
        val binding = useInScope(NettySyncServer().port(0).addEndpoint(endpoint).start())(_.stop())
        portRef.set(binding.port)
        serverShutdown = () => shutdown.countDown()
        ready.countDown()
        shutdown.await()

    ready.await()
    val port = portRef.get()

    backend = DefaultSyncBackend()
    val transport = ClientHttpTransport[Identity](backend, uri"http://localhost:$port/mcp")
    client = Iterator
      .continually(Try(McpClient[Identity](transport, Implementation("test-client", "0.0.1"))))
      .flatMap(_.toOption)
      .next()

  override def afterAll(): Unit =
    client.close()
    backend.close()
    serverShutdown()
