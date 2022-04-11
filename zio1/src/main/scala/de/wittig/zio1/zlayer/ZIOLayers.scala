package de.wittig.zio1.zlayer

import zio.{Ref, Runtime, ULayer, ZIO, ZLayer}

/** https://www.youtube.com/watch?v=u5IrfkAo6nk&list=WL&index=38&t=64s */
object ZIOLayers extends App:

  def show(message: String): ZIO[Show, Nothing, Unit] =
    Show.display(message)
//    ZIO.accessM[Has[Show]](_.get.display(message))
//    ZIO.service[Show].flatMap(_.display(message))

  trait Show:
    def display(message: String): ZIO[Any, Nothing, Unit]

  object Show:
    def display(message: String): ZIO[Show, Nothing, Unit] = ZIO.environmentWithZIO[Show](_.get.display(message))

  // Implementation 1
  val layer1: ZLayer[Any, Nothing, Show] = ZLayer.succeed(new Show {
    override def display(message: String): ZIO[Any, Nothing, Unit] = ZIO.succeed(println(message))
  })

  // Implementation 2
  case class ShowTest(lines: Ref[List[String]]) extends Show:
    override def display(message: String): ZIO[Any, Nothing, Unit] = lines.update(_ :+ message)

  def layer2(ref: Ref[List[String]]): ULayer[Show] = ZLayer.succeed(ShowTest(ref))

  // Using Layers
  val showUsingLayer1: ZIO[Any, Nothing, Unit]    = show("Hallo Welt").provideLayer(layer1)
  val showUsingLayer2: ZIO[Any, Nothing, Boolean] =
    for
      state <- Ref.make(List.empty[String])
      _     <- (show("Hallo") *> show("Welt")).provideLayer(layer2(state))
      lines <- state.get
    yield lines == List("Hallo", "Welt")

  // execute
  val runtime = Runtime.default

  runtime.unsafeRun(showUsingLayer1)
  runtime.unsafeRun(showUsingLayer2)
