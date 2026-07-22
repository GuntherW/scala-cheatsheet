package webapp

import ox.*
import sttp.tapir.server.netty.sync.NettySyncServer

object WebApp:
  private val port: Int = sys.env.getOrElse("PORT", "8080").toInt

  @main
  def run(): Unit =
    supervised {
      val binding = useInScope(
        NettySyncServer()
          .port(port)
          .addEndpoints(Routes.allServerEndpoints)
          .start()
      )(_.stop())
      println(s"Server started on http://localhost:${binding.port}")
      never
    }
