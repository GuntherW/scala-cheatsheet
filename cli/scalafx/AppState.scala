import scalafx.beans.property.{IntegerProperty, StringProperty}
import scalafx.collections.ObservableBuffer

// Zentraler Zustand der Anwendung — reaktive Properties, kein UI
class AppState:
  val counter       = IntegerProperty(0)
  val inputText     = StringProperty("")
  val statusMessage = StringProperty("Bereit.")
  val itemList      = ObservableBuffer[String]()
