/** Dieselbe Dummy-Formel wie im Manual-Orchestrator (`350 * teamSize + 500`).
  *
  * Orca registriert keine eigenen Tools. Der Risk-Analyst bekommt das JSON im Prompt, er ruft nichts auf.
  */
object CalculateTco:

  val DemoTeamSize = 5

  def estimate(technology: String, teamSize: Int = DemoTeamSize): String =
    val monthlyCostEur = 350 * teamSize + 500
    val note           = "Demo-Berechnung mit Dummy-Zahlen, keine reale Kostenanalyse."
    s"""{"technology":${json(technology)},"team_size":$teamSize,"estimated_monthly_cost_eur":$monthlyCostEur,"note":${json(note)}}"""

  private def json(value: String): String =
    val escaped = value.flatMap {
      case '\\'              => "\\\\"
      case '"'               => "\\\""
      case '\n'              => "\\n"
      case '\r'              => "\\r"
      case '\t'              => "\\t"
      case '\b'              => "\\b"
      case '\f'              => "\\f"
      case c if c < '\u0020' => f"\\u${c.toInt}%04x"
      case c                 => c.toString
    }
    s"\"$escaped\""
