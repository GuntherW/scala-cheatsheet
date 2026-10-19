# Staging (`scala3-staging`)

Offizielle Doku: https://docs.scala-lang.org/scala3/reference/metaprogramming/staging.html

## Was ist das?

Macros (`inline` + `${ ... }`) laufen zur **Compile-Zeit**. Staging nutzt dieselben Quotes (`'{ ... }`) und Splices (`${ ... }`),
führt sie aber zur **Laufzeit** aus: Das Programm erzeugt Code, kompiliert ihn mit einem eingebetteten Scala-Compiler
und führt ihn aus (`staging.run`). Mit `staging.withQuotes(_.show)` lässt sich der Code auch nur als String ausgeben.

## Wann nutzt man es?

- Die Eingabe für die Codegenerierung ist erst **zur Laufzeit** bekannt (Konfiguration, Benutzereingabe, Datei, Regeln aus DB).
- **Spezialisierung / Partial Evaluation**: Schleifen abrollen, Interpreter-Overhead entfernen, z. B. Regel-Engines,
  Formel-Evaluatoren, Matrix-/Polynom-Kerne mit fester Größe.
- DSLs, die zu schnellem JVM-Bytecode statt zu einem Interpreter kompiliert werden sollen.
- Experimente und Lehre: Multi-Stage Programming mit typsicheren Code-Fragmenten (`Expr[T]`).

## Wann nicht?

- Wenn die Eingaben schon zur Compile-Zeit feststehen: normale `inline`-Macros nehmen. Kein Compiler zur Laufzeit nötig.
- Bei kurzlebigen Programmen/Einmalaufrufen: `Compiler.make` und das Kompilieren kosten Hunderte ms bis Sekunden;
  es lohnt sich nur, wenn der erzeugte Code oft aufgerufen wird.
- Wenn die Anwendung schlank bleiben soll: `scala3-staging` bringt den kompletten Compiler (`scala3-compiler`) in den Classpath.

## Wichtige Punkte

- Dependency: `"org.scala-lang" %% "scala3-staging" % scalaVersion` (hier `Library.scala3Staging`).
- Ein `given Compiler = Compiler.make(getClassLoader)` wird benötigt. Instanz wiederverwenden, sie ist teuer.
- Typsicherheit: `Expr[T]` stellt sicher, dass der generierte Code wohlgetypt ist (Fehler früher als bei String-basiertem Codegen).
- Der Quotes-Kontext (`Quotes`) ist nur innerhalb von `run` / `withQuotes` gültig; `Expr` darf nicht daraus entweichen.

## Beispiele

Zwei voneinander unabhängige Demos:

- `UnrolledPowerDemo.scala`: erzeugt eine ausgerollte `power`-Funktion für einen zur Laufzeit gewählten Exponenten.
- `MembershipDemo.scala`: erzeugt aus einer zur Laufzeit gegebenen Liste einen Membership-Test (`i == 1 || i == 3 || ...`).

```
sbt --client "macros/runMain de.wittig.macros.staging.runUnrolledPowerDemo 4"
sbt --client "macros/runMain de.wittig.macros.staging.runMembershipDemo 1 3 5"
```
