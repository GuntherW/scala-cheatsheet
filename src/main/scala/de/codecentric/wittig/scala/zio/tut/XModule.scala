package de.codecentric.wittig.scala.zio.tut
import zio._

trait Service[R] {
  def x: ZIO[R, Nothing, X]
}

trait XModule {
  val xModule: Service[Any]
}

case class X(
    x: String = "x",
    y: String = "y",
    z: String = "z"
)

object XModule {
  trait Live extends XModule {
    val xInstance: X

    val xModule: Service[Any] = new Service[Any] {
      override def x: ZIO[Any, Nothing, X] = UIO(xInstance)
    }
  }

  object factory extends Service[XModule] {
    override def x: ZIO[XModule, Nothing, X] = ZIO.environment[XModule].flatMap(_.xModule.x)
  }
}
