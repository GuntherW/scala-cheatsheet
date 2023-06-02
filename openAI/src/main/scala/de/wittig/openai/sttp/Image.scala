package de.wittig.openai.sttp

import sttp.client4.*
import sttp.openai.OpenAISyncClient
import sttp.openai.requests.completions.chat.ChatRequestBody.{ChatBody, ChatCompletionModel}
import sttp.openai.requests.completions.chat.ChatRequestResponseData.ChatResponse
import sttp.openai.requests.completions.chat.{Message, Role}
import sttp.openai.requests.images.{ResponseFormat, Size}
import sttp.openai.requests.images.creation.ImageCreationRequestBody
import sttp.openai.requests.images.creation.ImageCreationRequestBody.ImageCreationBody

object Image extends App {

  private val openAI = OpenAISyncClient(sys.env("OPENAI_APIKEY"))

  private val imageRequestBody = ImageCreationBody(
    prompt = "cute fish",
    n = Some(2),
    size = Some(Size.Custom("1024x1024")),
    responseFormat = Some(ResponseFormat.URL),
    user = Some("Gunther")
  )

  private val imageResponse = openAI.createImage(imageRequestBody)

  imageResponse
    .data
    .map(_.url)
    .foreach(println)
}
