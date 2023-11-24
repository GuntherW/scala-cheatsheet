package de.wittig.openai.sttp

import sttp.openai.OpenAISyncClient
import sttp.openai.requests.completions.chat.ChatRequestBody.{ChatBody, ChatCompletionModel}
import sttp.openai.requests.completions.chat.ChatRequestResponseData.ChatResponse
import sttp.openai.requests.completions.chat.message.*

object Chat extends App {

  private val openAI: OpenAISyncClient = OpenAISyncClient(sys.env("OPENAI_APIKEY"))

  private val requestMessage = Seq(
    Message.UserMessage(
      content = Content.TextContent("Hello!")
    )
  )

  private val chatRequestBody = ChatBody(
    model = ChatCompletionModel.GPT35Turbo,
    messages = requestMessage
  )

  private val chatResponse: ChatResponse = openAI.createChatCompletion(chatRequestBody)
  println(chatResponse)
}
