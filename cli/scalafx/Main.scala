//> using dep "org.scalafx::scalafx:23.0.1-R34"

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.layout.VBox
import scalafx.scene.control.Label
import scalafx.geometry.Pos.Center


object Main extends JFXApp3:

    override def start():Unit = 
        stage = new JFXApp3.PrimaryStage:
            width = 640
            height = 480
            scene = new Scene:
                root = new VBox:
                    alignment = Center
                    children = Label("Hallo Welt")