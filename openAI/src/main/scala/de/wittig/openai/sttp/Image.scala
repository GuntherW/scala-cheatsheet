package de.wittig.openai.sttp

import sttp.openai.OpenAISyncClient
import sttp.openai.requests.images.creation.ImageCreationRequestBody
import sttp.openai.requests.images.creation.ImageCreationRequestBody.ImageCreationBody
import sttp.openai.requests.images.{ResponseFormat, Size}

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
