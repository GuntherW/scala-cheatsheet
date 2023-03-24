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
