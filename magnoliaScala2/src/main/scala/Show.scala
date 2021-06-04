trait Show[T] {
  def show(t: T): String
}

object Show {
  implicit val showInt: Show[Int]       = (t: Int) => t.toString
  implicit val showString: Show[String] = (t: String) => t
}
