package sub1
import sub2._

object SubProject1 {
  def hallo = s"Hallo von Subproject1 - ${SubProject2.hallo}"
}
