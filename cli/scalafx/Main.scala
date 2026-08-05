//> using file AppState.scala
//> using file CounterView.scala
//> using file ItemListView.scala
//> using dep "org.scalafx::scalafx:26.0.0-R38"

import scalafx.application.JFXApp3
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.Label
import scalafx.scene.layout.{BorderPane, HBox, VBox}

object Main extends JFXApp3:

  override def start(): Unit =
    val state = AppState()

    val header = new HBox:
      alignment = Pos.Center
      padding = Insets(12)
      children = new Label("ScalaFX Demo"):
        style = "-fx-font-size: 20px; -fx-font-weight: bold;"

    val statusBar = new Label:
      text <== state.statusMessage
      padding = Insets(6, 10, 6, 10)
      maxWidth = Double.MaxValue
      style = "-fx-background-color: #eeeeee; -fx-font-style: italic;"

    val mainRoot = new BorderPane:
      top = header
      center = new VBox(CounterView(state), ItemListView(state))
      bottom = statusBar

    stage = new JFXApp3.PrimaryStage:
      title = "ScalaFX Demo"
      width = 500
      height = 420
      scene = new Scene:
        root = mainRoot
