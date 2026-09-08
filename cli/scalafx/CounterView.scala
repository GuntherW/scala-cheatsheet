import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.HBox

// View-Klasse: empfängt State, besitzt keine eigene Logik außer UI-Aufbau
class CounterView(state: AppState) extends HBox(10):
  alignment = Pos.Center
  padding = Insets(8)

  private val counterLabel = new Label:
    text <== state.counter.asString("Zähler: %d")

  private def updateCounter(delta: Int, successMessage: Int => String, failMessage: => String): Unit =
    if delta < 0 && state.counter.value <= 0 then state.statusMessage.value = failMessage
    else
      state.counter.value += delta
      state.statusMessage.value = successMessage(state.counter.value)

  private val incrementBtn = new Button("+ Erhöhen"):
    onAction = _ => updateCounter(1, n => s"Zähler erhöht auf $n", "")

  private val decrementBtn = new Button("- Verringern"):
    onAction = _ => updateCounter(-1, n => s"Zähler verringert auf $n", "Zähler ist bereits 0.")

  private val resetBtn = new Button("Zurücksetzen"):
    style = "-fx-text-fill: red;"
    onAction = _ =>
      state.counter.value = 0
      state.statusMessage.value = "Zähler zurückgesetzt."

  children = List(decrementBtn, counterLabel, incrementBtn, resetBtn)
