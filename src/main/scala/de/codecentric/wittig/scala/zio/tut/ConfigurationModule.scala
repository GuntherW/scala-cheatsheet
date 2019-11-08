package de.codecentric.wittig.scala.zio.tut
import de.codecentric.wittig.scala.zio.tut.config.{Configuration}
import pureconfig._
import pureconfig.generic.auto._
import zio._

trait ConfigurationModule {
  val configurationModule: ConfigurationModule.Service[Any]
}

object ConfigurationModule {
  case class ConfigurationError(message: String) extends RuntimeException(message)

  trait Service[R] {
    def configuration: ZIO[R, Throwable, Configuration]
  }

  trait Live extends ConfigurationModule {
    val configurationModule: ConfigurationModule.Service[Any] = new Service[Any] {
      override def configuration: Task[Configuration] =
        ZIO
          .fromEither(ConfigSource.default.load[Configuration])
          .mapError(e => ConfigurationError(e.toList.mkString(", ")))
    }
  }

  object factory extends ConfigurationModule.Service[ConfigurationModule] {
    override def configuration: ZIO[ConfigurationModule, Throwable, Configuration] = ZIO.accessM[ConfigurationModule](_.configurationModule.configuration)
  }
}
