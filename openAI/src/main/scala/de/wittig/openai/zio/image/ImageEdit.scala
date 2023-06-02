package de.wittig.openai.zio.image

import zio.{Chunk, Console, ZIO, ZIOAppDefault}
import zio.nio.file.{Files, Path}
import zio.openai.Images
import zio.openai.model.{File, N, ResponseFormat, Size}

import java.util.Base64

/** Based on https://beta.openai.com/docs/api-reference/images
  */
object ImageEdit extends ZIOAppDefault {

  private def createImageEdit =
    for {
      input    <- File.read(Path("openAI/ZIO.png"))
      response <- Images.createImageEdit(
                    image = input,
                    prompt = "ZIO logo with a dog",
                    n = N(2),
                    size = Size.`512x512`,
                    responseFormat = ResponseFormat.B64_json
                  )
      _        <- ZIO.foreachDiscard(response.data.zipWithIndex) {
                    case (data, idx) =>
                      val imageData = Base64.getDecoder.decode(data.b64Json.getOrElse(""))
                      val path      = Path(s"openAI/editedImage-$idx.png")

                      Files.writeBytes(path, Chunk.fromArray(imageData)) *> Console.printLine(s"Image written to $path")
                  }
    } yield ()

  def run = createImageEdit.provide(Images.default)
}
