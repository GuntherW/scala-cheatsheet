import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, ListView, TextField}
import scalafx.scene.layout.{HBox, VBox}

class ItemListView(state: AppState) extends VBox(4):
  padding = Insets(8)

  private val inputField = new TextField:
    promptText = "Element eingeben..."
    text <==> state.inputText

  private val addBtn = new Button("Hinzufügen"):
    onAction = _ =>
      val trimmed = state.inputText.value.trim
      if trimmed.nonEmpty then
        state.itemList += trimmed
        state.inputText.value = ""
        state.statusMessage.value = s"'$trimmed' hinzugefügt."
      else state.statusMessage.value = "Eingabe darf nicht leer sein."

  private val removeBtn = new Button("Entfernen"):
    onAction = _ =>
      if state.itemList.nonEmpty then
        val removed = state.itemList.last
        state.itemList.remove(state.itemList.size - 1)
        state.statusMessage.value = s"'$removed' entfernt."
      else state.statusMessage.value = "Liste ist leer."

  private val inputRow = new HBox(8):
    alignment = Pos.CenterLeft
    children = List(inputField, addBtn, removeBtn)

  private val listView = new ListView[String]:
    items = state.itemList
    prefHeight = 180

  children = List(inputRow, listView)
