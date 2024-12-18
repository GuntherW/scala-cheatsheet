//> using dep "com.softwaremill.ox::core::0.5.7"

import ox.*

@main def main = {

  List(1, 2, 3).mapPar(2)(identity).foreach(println)
}
