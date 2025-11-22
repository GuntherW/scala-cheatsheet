package de.wittig.openai.sttp

import sttp.openai.OpenAISyncClient
import sttp.openai.requests.images.creation.ImageCreationRequestBody.{ImageCreationBody, ImageCreationModel}
import sttp.openai.requests.images.{ResponseFormat, Size}

@main
def image(): Unit =

  val openAI = OpenAISyncClient(sys.env("OPENAI_APIKEY"))

  val imageRequestBody = ImageCreationBody(
    prompt = "cute fish",
    model = ImageCreationModel.DALLE3,
    n = Some(1),
    size = Some(Size.Custom("1024x1024")),
    responseFormat = Some(ResponseFormat.URL),
    user = Some("Gunther")
  )

  val imageResponse = openAI.createImage(imageRequestBody)

  imageResponse
    .data
    .map(_.url)
    .foreach(println)
