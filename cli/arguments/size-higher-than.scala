//> using dep com.lihaoyi::os-lib::0.11.8

import os.Path

@main
def sizeHigherThan(dir: String, minSizeMB: Int): Unit =
  val minSizeBytes = minSizeMB.toLong * 1_000_000L
  os.walk
    .attrs(Path(dir))
    .collect {
      case (p, attrs) if attrs.size > minSizeBytes => p
    }
    .foreach(println)
