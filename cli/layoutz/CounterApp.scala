//> using dep xyz.matthieucourt::layoutz::0.6.0

import layoutz.*

@main
def counterApp(): Unit = CounterApp.run()

object CounterApp extends LayoutzApp[Int, String] {
  def init = 0

  def update(msg: String, count: Int) = msg match
    case "inc" => count + 1
    case "dec" => count - 1
    case _     => count

  def subscriptions(count: Int) =
    Sub.onKeyPress {
      case CharKey('+') => Some("inc")
      case CharKey('-') => Some("dec")
      case _            => None
    }

  def view(count: Int) = layout(
    statusCard("Count", count.toString).border(Border.Thick),
    inlineBar("Count", count.toDouble / 100),
    br,
    ul("Press `+` or `-`")
  )
}
