private val header = "scala-cli is fast"
private val footer = "scala-cli allows packaging"

def renderText(body: String): String =
  s"""
    |  $header
    |
    |  $body
    |
    |  $footer
    """.trim.stripMargin
