package de.wittig.os

object Main extends App:

  println(s"pwd : ${os.pwd}")
  println(s"root: ${os.root}")
  println(s"home: ${os.home}")
  println(s"up  : ${os.up}")

  val txtFiles = os.list(os.pwd).find(_.ext == "txt")
  println(s"list pwd: $txtFiles}")
  println(os.read(txtFiles.head))

  val wd      = os.pwd / "osLib" / "testordner"
  os.remove.all(wd)
  os.makeDir.all(wd)
  os.write(wd / "file.txt", "hello")
  val content = os.read(wd / "file.txt")
  println(s"file content: $content")
  os.copy(wd / "file.txt", wd / "copied.txt")
  println(os.list(wd))

  // Invoke subprocesses
  val invoked = os.proc("cat", wd / "file.txt", wd / "copied.txt").call(cwd = wd)
  println(invoked.out.trim())

  // Chain multiple subprocess' stdin/stdout together
  val curl = os.proc("curl", "-L", "https://www.google.de").spawn(stderr = os.Inherit)
  val gzip = os.proc("gzip", "-n").spawn(stdin = curl.stdout)
  val sha  = os.proc("shasum", "-a", "256").spawn(stdin = gzip.stdout)
  println(sha.stdout.trim())
  println("-" * 10)

  // Find and concatenate all .txt files directly in the working directory
  os.write(
    wd / "all.txt",
    os.list(wd).filter(_.ext == "txt").map(os.read)
  )
  println(os.read(wd / "all.txt"))

  // Line-count of all .txt files recursively in wd
  val lineCount = os.walk(wd)
    .filter(_.ext == "txt")
    .map(os.read.lines)
    .map(_.size)
    .sum
  println(lineCount)

  // Find the largest two files in the given folder tree
  val largestTwo = os.walk(wd)
    .filter(os.isFile(_, followLinks = false))
    .map(x => os.size(x) -> x).sortBy(-_._1)
    .take(2)
  println(largestTwo)
