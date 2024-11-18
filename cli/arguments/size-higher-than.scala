//> using dep com.lihaoyi::os-lib::0.11.3

import os.Path

@main
def sizeHigherThan(dir: String, minSizeMB: Int) =
  os.walk
    .attrs(Path(dir))
    .collect {
      case (p, attrs) if attrs.size > minSizeMB * 10e6 => p
    }
    .foreach(println)
