//> using dep xyz.matthieucourt::layoutz::0.3.0

import layoutz.*

@main
def layoutDemo = println(dashboard.render)

def dashboard =
  layout(
    section("System Status")(
      row( // layout horizontal
        statusCard("CPU", "45%"),
        statusCard("Memory", "78%"),
        statusCard("Disk", "23%")
      )
    ),
    br, // line break

    "Andere Sektionen:",
    box("Recent Activity")(
      ul(
        "User alice logged in",
        "Database backup completed",
        "3 new deployments"
      )
    ),
    hr, // horizontal rule
    hr.width(30).char("~"),
    inlineBar("Health", 0.91),
    section("Config")(kv("env" -> "prod", "version" -> "1.2.3")),
    table(
      headers = Seq("Name", "Status"),
      rows = Seq(Seq("Alice", "Online"), Seq("Bob", "Away"))
    ),
    tree("Project")(
      tree("src")(
        tree("main")(tree("App.scala")),
        tree("test")(tree("AppSpec.scala"))
      )
    )
  )
