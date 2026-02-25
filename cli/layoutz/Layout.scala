//> using dep xyz.matthieucourt::layoutz::0.6.0

import layoutz.*

@main
def layoutDemo = println(dashboard.render)

/** * Define layouts **
  */
val t = table(
  Seq("Name", "Role", "Status"),
  Seq(
    Seq("Alice", "Engineer", "Online"),
    Seq("Eve", "QA", "Away"),
    Seq(ul("Gegard", ul("Mousasi", ul("was a BAD man"))), "Fighter", "Nasty")
  )
).border(Border.Round)

/** * Nest, compose, combine them **
  */
val dashboard = layout(
  row(
    underline("^", Color.BrightMagenta)("Layoutz").style(Style.Bold),
    "... A Small Demo (ちいさい)"
  ).center(),
  row(
    statusCard("Users", "1.2K").color(Color.BrightBlue),
    statusCard("API", "UP").border(Border.Double).color(Color.BrightGreen),
    statusCard("CPU", "23%").border(Border.Thick).color(Color.BrightYellow),
    t,
    section("Pugilists")(
      layout(
        kv("Kazushi" -> "Sakuraba", "Jet 李連杰" -> "Li", "Rory" -> "MacDonald"),
        tightRow((0 to 255 by 12).map { i =>
          val r = if (i < 128) i * 2 else 255;
          val g = if (i < 128) 255 else (255 - i) * 2
          val b = if (i > 128) (i - 128) * 2 else 0;
          "█".color(Color.True(r, g, b))
        }*)
      )
    )
  ),
  row(
    layout(
      box("Wrapped")(wrap("Where there is a will ... Water x Necessaries", 20))
        .color(Color.BrightMagenta)
        .style(Style.Reverse ++ Style.Bold),
      ol("Arcole", "Austerlitz", ol("Iéna", ol("Бородино")))
    ),
    margin("[Scala!]")(
      box("Deploy Status")(
        inlineBar("Build", 1.0),
        inlineBar("Test", 0.8),
        inlineBar("Deploy", 0.3)
      ).color(Color.Green),
      tree("📁 Project")(
        tree("src")(
          tree("main.scala"),
          tree("test.scala")
        )
      ).color(Color.Cyan)
    )
  )
)
