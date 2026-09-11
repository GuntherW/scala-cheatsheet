Der Dateiname (scala-reviewer.md) wird der Agent-Name — aufrufbar mit @scala-reviewer.

scala-reviewer als Agent (nicht Skill):

Eigener Kontext: Agents laufen als Subagent mit eigenem Kontextfenster/eigener Session (task-Tool). Ein Skill lädt nur Anweisungen in die aktuelle Session.
Eigene Permissions: permission.edit/write/patch: deny, nur scalex * als Bash erlaubt. Das ist strikte Sandbox-Isolation — als Skill hättest du das nicht, der Haupt-Agent behält seine vollen Rechte.
Eigenes Modell/Temperatur: temperature: 0.1 für konsistentere Reviews — pro Skill nicht konfigurierbar.
Klare Rollenabgrenzung: "Review only, nie editieren" passt zum Subagent-Modell (isolierte, fokussierte Aufgabe mit definiertem Output), nicht zu einem Skill, das eher Workflow-Wissen für die Hauptsession bereitstellt.

Ist das sinnvoll? Ja. Faustregel:

Skill = "Wie mache ich X" (Wissen/Workflow, gleicher Kontext, gleiche Rechte)
Agent = "Lass wen anderen X machen" (isolierter Kontext, eigene Rechte/Modell, klar abgegrenzte Aufgabe mit Rückgabewert)