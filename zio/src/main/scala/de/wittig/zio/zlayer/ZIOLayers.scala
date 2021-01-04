package de.wittig.zio.zlayer

import zio.{Has, Ref, Runtime, ULayer, ZIO, ZLayer}

/** https://www.youtube.com/watch?v=u5IrfkAo6nk&list=WL&index=38&t=64s */
object ZIOLayers extends App {

  def show(message: String): ZIO[Has[Show], Nothing, Unit] = {
    Show.display(message)
//    ZIO.accessM[Has[Show]](_.get.display(message))
//    ZIO.service[Show].flatMap(_.display(message))
  }

  trait Show {
    def display(message: String): ZIO[Any, Nothing, Unit]
  }

  object Show {
    def display(message: String): ZIO[Has[Show], Nothing, Unit] = ZIO.accessM[Has[Show]](_.get.display(message))
  }

  // Implementation 1
  val layer1: ZLayer[Any, Nothing, Has[Show]] = ZLayer.succeed(new Show {
    override def display(message: String): ZIO[Any, Nothing, Unit] = ZIO.effectTotal(println(message))
  })

  // Implementation 2
  case class ShowTest(lines: Ref[List[String]]) extends Show {
    override def display(message: String): ZIO[Any, Nothing, Unit] =
      lines.update(_ :+ message)
  }
  def layer2(ref: Ref[List[String]]): ULayer[Has[Show]] = ZLayer.succeed(ShowTest(ref))

  // Implementation 3
  case class EmbroideryConfig(art: Char, spaces: Int)
  val layer3: ZLayer[Has[EmbroideryConfig], Nothing, Has[Show]] = ZLayer.fromService(config =>
    new Show {
      import embroidery._
      override def display(message: String): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal(println(message.toAsciiArt(config.art, config.spaces)))
    }
  )

  // Using Layers
  val showUsingLayer1: ZIO[Any, Nothing, Unit]    = show("Hallo Welt").provideLayer(layer1)
  val showUsingLayer2: ZIO[Any, Nothing, Boolean] =
    for {
      state <- Ref.make(List.empty[String])
      _     <- (show("Hallo") *> show("Welt")).provideLayer(layer2(state))
      lines <- state.get
    } yield lines == List("Hallo", "Welt")

  val showUsingLayer3: ZIO[Any, Nothing, Unit] = {
    val configLayer = ZLayer.succeed(EmbroideryConfig(art = 'ö', spaces = 1))
    show("Hallo Welt").provideLayer(configLayer >>> layer3)
  }

  // execute
  val runtime = Runtime.default

  runtime.unsafeRun(showUsingLayer1)
  runtime.unsafeRun(showUsingLayer2)
  runtime.unsafeRun(showUsingLayer3)
}
