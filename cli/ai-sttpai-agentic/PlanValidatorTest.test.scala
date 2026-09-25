package agents

class PlanValidatorTest extends munit.FunSuite:

  private def spec(id: String, dependsOn: Set[String] = Set.empty, mandatory: Boolean = false): AgentSpec =
    AgentSpec(id = id, description = id, hardDependsOn = dependsOn, isMandatory = mandatory, execute = _ => id)

  private val a        = spec("a")
  private val b        = spec("b")
  private val c        = spec("c", dependsOn = Set("a", "b"), mandatory = true)
  private val registry = Map(a.id -> a, b.id -> b, c.id -> c)

  test("korrekter Plan bleibt unverändert (Levels + finalAgentId)") {
    val plan   = ExecutionPlan(steps = List(List("a", "b"), List("c")), finalAgentId = "c", reasoning = "ok")
    val result = PlanValidator.validate(plan, registry)
    assertEquals(result.steps, List(List("a", "b"), List("c")))
    assertEquals(result.finalAgentId, "c")
  }

  test("unbekannte agent-ids werden entfernt") {
    val plan   = ExecutionPlan(steps = List(List("a", "unknown", "b"), List("c")), finalAgentId = "c", reasoning = "")
    val result = PlanValidator.validate(plan, registry)
    assertEquals(result.steps, List(List("a", "b"), List("c")))
  }

  test("doppelte agent-ids werden dedupliziert") {
    val plan   = ExecutionPlan(steps = List(List("a"), List("a", "b"), List("c")), finalAgentId = "c", reasoning = "")
    val result = PlanValidator.validate(plan, registry)
    assertEquals(result.steps.flatten.count(_ == "a"), 1)
  }

  test("fehlender Pflicht-Agent wird ergänzt") {
    val plan   = ExecutionPlan(steps = List(List("a", "b")), finalAgentId = "b", reasoning = "")
    val result = PlanValidator.validate(plan, registry)
    assertEquals(result.steps, List(List("a", "b"), List("c")))
    assertEquals(result.finalAgentId, "b") // vom Aufrufer explizit gewählte, gültige finalAgentId bleibt erhalten
  }

  test("verletzte Abhängigkeit wird repariert (c vor seinen Deps landet trotzdem danach)") {
    val plan   = ExecutionPlan(steps = List(List("c", "a", "b")), finalAgentId = "c", reasoning = "")
    val result = PlanValidator.validate(plan, registry)
    assertEquals(result.steps, List(List("a", "b"), List("c")))
  }

  test("ungültige finalAgentId fällt auf Pflicht-Agenten zurück") {
    val plan   = ExecutionPlan(steps = List(List("a", "b"), List("c")), finalAgentId = "does-not-exist", reasoning = "")
    val result = PlanValidator.validate(plan, registry)
    assertEquals(result.finalAgentId, "c")
  }

  test("leerer Plan erzeugt trotzdem einen gültigen Plan mit allen Pflicht-Agenten") {
    val plan   = ExecutionPlan(steps = Nil, finalAgentId = "", reasoning = "")
    val result = PlanValidator.validate(plan, registry)
    assertEquals(result.steps.flatten.toSet, Set("c"))
    assertEquals(result.finalAgentId, "c")
  }

  test("Zyklus in hardDependsOn wird defensiv aufgebrochen statt zu blockieren") {
    val x         = spec("x", dependsOn = Set("y"))
    val y         = spec("y", dependsOn = Set("x"))
    val cyclicReg = Map(x.id -> x, y.id -> y)
    val plan      = ExecutionPlan(steps = List(List("x", "y")), finalAgentId = "y", reasoning = "")
    val result    = PlanValidator.validate(plan, cyclicReg)
    assertEquals(result.steps.flatten.toSet, Set("x", "y"))
  }
