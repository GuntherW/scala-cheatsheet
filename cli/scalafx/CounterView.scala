import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.HBox

// View-Klasse: empfängt State, besitzt keine eigene Logik außer UI-Aufbau
class CounterView(state: AppState) extends HBox(10):
  alignment = Pos.Center
  padding = Insets(8)

  private val counterLabel = new Label:
    text <== state.counter.asString("Zähler: %d")

  private val incrementBtn = new Button("+ Erhöhen"):
    onAction = _ =>
      state.counter.value += 1
      state.statusMessage.value = s"Zähler erhöht auf ${state.counter.value}"

  private val decrementBtn = new Button("- Verringern"):
    onAction = _ =>
      if state.counter.value > 0 then
        state.counter.value -= 1
        state.statusMessage.value = s"Zähler verringert auf ${state.counter.value}"
      else state.statusMessage.value = "Zähler ist bereits 0."

  private val resetBtn = new Button("Zurücksetzen"):
    style = "-fx-text-fill: red;"
    onAction = _ =>
      state.counter.value = 0
      state.statusMessage.value = "Zähler zurückgesetzt."

  children = List(decrementBtn, counterLabel, incrementBtn, resetBtn)
