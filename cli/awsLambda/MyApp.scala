//> using jvm "21"
//> using repository "jitpack"
//> using dep "com.github.lambdaspot:aws-lambda-scala-bridge:0.1.5"
//> using dep "com.amazonaws:aws-lambda-java-core:1.2.3"
//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:2.25.0"
//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.25.0"

import com.amazonaws.services.lambda.runtime.Context
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.lambdaspot.aws.lambda.core.*
import dev.lambdaspot.aws.lambda.events.ApiGatewayProxiedResponse
import scala.util.{Success, Try}

object MyApp extends AwsLambdaEntryPoint:

  override lazy val entryPoint = new AwsLambda[PersonDto, ApiGatewayProxiedResponse]:
    override def run(person: PersonDto, context: Context): Try[ApiGatewayProxiedResponse] =
      context.getLogger.log(s"Request received: $person\n")
      Success(
        ApiGatewayProxiedResponse(
          statusCode = 200,
          headers = Map("Content-Type" -> "text/plain"),
          body = Some(s"Hallo ${person.name}!")
        )
      )

case class PersonDto(name: String, age: Int)
object PersonDto:
  given JsonValueCodec[PersonDto] = JsonCodecMaker.make
