package de.wittig.openai.sttp

import sttp.openai.OpenAISyncClient
import sttp.openai.requests.completions.chat.ChatRequestBody.{ChatBody, ChatCompletionModel}
import sttp.openai.requests.completions.chat.ChatRequestResponseData.ChatResponse
import sttp.openai.requests.completions.chat.message.*

@main
def chat(): Unit =

  val openAI: OpenAISyncClient = OpenAISyncClient(sys.env("OPENAI_APIKEY"))

  val requestMessage = Seq(
    Message.UserMessage(
      content = Content.TextContent("Hello!")
    )
  )

  val chatRequestBody = ChatBody(
    model = ChatCompletionModel.GPT35Turbo,
    messages = requestMessage
  )

  val chatResponse: ChatResponse = openAI.createChatCompletion(chatRequestBody)
  println(chatResponse)
