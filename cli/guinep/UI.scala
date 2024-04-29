//> using dep io.github.kacperfkorban::guinep-web:0.1.0
//> using toolkit default

def upperCaseText(text: String): String = text.toUpperCase
def rollDice(sides: Int): Int = scala.util.Random.nextInt(sides) + 1

case class Add(a: Int, b: Int)
def addObj(add: Add) = add.a + add.b

object UI extends App:
  guinep.web(
    upperCaseText,
    rollDice,
    addObj
  )
