//> using dep xyz.matthieucourt::layoutz::0.1.0

import layoutz.*

@main
def simple = {

   val dashboard = layout(
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
      bullets(
        "User alice logged in",
        "Database backup completed",
        "3 new deployments"
      )
    ),
    hr,  // horizontal rule
    hr("~", 30),
    inlineBar("Health", 0.91),
    section("Config")(kv("env" -> "prod", "version" -> "1.2.3")),
    table(
      headers = Seq("Name", "Status"),
      rows = Seq(Seq("Alice", "Online"), Seq("Bob", "Away"))
    ),
    tree("Project")(
      branch("src",
        branch("main", leaf("App.scala")),
        branch("test", leaf("AppSpec.scala"))
      )
    )
  )
  
  println(dashboard.render)
}