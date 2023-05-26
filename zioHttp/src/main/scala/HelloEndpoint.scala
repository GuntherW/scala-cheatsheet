import zio.*
import zio.http.*
import zio.http.codec.HttpCodec.{literal, queryInt, string}
import zio.http.endpoint.*
import zio.http.model.Method
import zio.schema.{DeriveSchema, Schema}
import zio.stream.ZStream

object HelloEndpoint extends ZIOAppDefault:

  private val endpoint = Endpoint
    .get(literal("first") / string("name"))
    .query(queryInt("age"))
    .out[ExampleResponse]

  private val endpointImpl = endpoint.implement {
    case (name, age) => ZIO.succeed(ExampleResponse(name, age))
  }

  private val app = endpointImpl.toApp

  override val run = Server
    .serve(app)
    .provide(
      ServerConfig.live(ServerConfig.default.port(8091)),
      Server.live
    )

final case class ExampleResponse(name: String, age: Int)
object ExampleResponse:
  given Schema[ExampleResponse] = DeriveSchema.gen[ExampleResponse]
