import scala.util.Try

// --- Datentypen (fachlich) ---

case class DirEntry(name: String, isDirectory: Boolean, sizeBytes: Option[Long])
case class FileContent(path: os.Path, sizeBytes: Long, text: String, totalLines: Int, offset: Int, limit: Option[Int])
case class SearchMatch(file: os.Path, lineNumber: Int, line: String)
case class FileMetadata(path: os.Path, isDirectory: Boolean, sizeBytes: Option[Long], lastModified: String, readable: Boolean, writable: Boolean)

// --- Fachliche Operationen ---

def listDirectory(path: os.Path): Either[String, List[DirEntry]] =
  if !os.exists(path) then Left(s"Path does not exist: $path")
  else if !os.isDir(path) then Left(s"Not a directory: $path")
  else
    Try {
      os.list(path).toList
        .sortBy(p => (!os.isDir(p), p.last))
        .map: p =>
          DirEntry(
            name = p.last,
            isDirectory = os.isDir(p),
            sizeBytes = if os.isDir(p) then None else Some(os.size(p))
          )
    }.toEither.left.map(_.getMessage)

def readFile(path: os.Path, offset: Int = 1, limit: Option[Int] = None): Either[String, FileContent] =
  if !os.exists(path) then Left(s"File does not exist: $path")
  else if os.isDir(path) then Left(s"Path is a directory, not a file: $path")
  else
    val size = os.size(path)
    if size > 10 * 1024 * 1024 then Left(s"File too large (${formatSize(size)}). Limit is 10 MB.")
    else
      Try(os.read.lines(path).toVector).toEither
        .left.map(e => s"Could not read file (might be binary): ${e.getMessage}")
        .map: allLines =>
          val from   = (offset - 1).max(0)
          val sliced = limit.fold(allLines.drop(from))(n => allLines.slice(from, from + n))
          FileContent(path, size, sliced.mkString("\n"), allLines.size, offset, limit)

def searchInFiles(basePath: os.Path, pattern: String, fileExtension: String, maxResults: Int): Either[String, List[SearchMatch]] =
  if !os.exists(basePath) then Left(s"Directory does not exist: $basePath")
  else if !os.isDir(basePath) then Left(s"Not a directory: $basePath")
  else
    Try {
      val regex    = pattern.r
      val allFiles = os.walk(basePath)
        .filter(os.isFile(_))
        .filter(p => fileExtension == "*" || p.last.endsWith(s".$fileExtension"))

      allFiles.iterator
        .flatMap: file =>
          Try(os.read.lines(file)).map(_.iterator.zipWithIndex).getOrElse(Iterator.empty)
            .collect { case (line, idx) if regex.findFirstIn(line).isDefined => SearchMatch(file, idx + 1, line) }
        .take(maxResults)
        .toList
    }.toEither.left.map(_.getMessage)

def fileInfo(path: os.Path): Either[String, FileMetadata] =
  if !os.exists(path) then Left(s"Path does not exist: $path")
  else
    Try {
      val stat = os.stat(path)
      FileMetadata(
        path = path,
        isDirectory = os.isDir(path),
        sizeBytes = if os.isDir(path) then None else Some(stat.size),
        lastModified = stat.mtime.toString,
        readable = path.toIO.canRead,
        writable = path.toIO.canWrite
      )
    }.toEither.left.map(_.getMessage)

def writeFile(path: os.Path, content: String, createDirs: Boolean = true): Either[String, Unit] =
  Try {
    if createDirs then os.makeDir.all(path / os.up)
    os.write.over(path, content)
  }.toEither.left.map(_.getMessage)

def editFile(path: os.Path, oldString: String, newString: String): Either[String, Int] =
  if !os.exists(path) then Left(s"File does not exist: $path")
  else if os.isDir(path) then Left(s"Path is a directory, not a file: $path")
  else
    Try(os.read(path)).toEither.left.map(_.getMessage).flatMap: content =>
      val count = countOccurrences(content, oldString)
      if count == 0 then Left(s"oldString not found in file: $path")
      else if count > 1 then Left(s"Found $count matches for oldString. Provide more surrounding context to identify the correct match.")
      else
        Try(os.write.over(path, content.replace(oldString, newString))).toEither
          .left.map(_.getMessage)
          .map(_ => count)

def globFiles(basePath: os.Path, pattern: String): Either[String, List[os.Path]] =
  if !os.exists(basePath) then Left(s"Path does not exist: $basePath")
  else
    Try {
      os.walk(basePath).filter(p => os.isFile(p) && p.last.matches(globToRegex(pattern))).toList.sorted
    }.toEither.left.map(_.getMessage)

def createDirectory(path: os.Path): Either[String, Unit] =
  Try(os.makeDir.all(path)).toEither.left.map(_.getMessage)

def movePath(from: os.Path, to: os.Path, createDirs: Boolean = true): Either[String, Unit] =
  if !os.exists(from) then Left(s"Source does not exist: $from")
  else
    Try {
      if createDirs then os.makeDir.all(to / os.up)
      os.move(from, to, replaceExisting = true)
    }.toEither.left.map(_.getMessage)

def copyPath(from: os.Path, to: os.Path, createDirs: Boolean = true): Either[String, Unit] =
  if !os.exists(from) then Left(s"Source does not exist: $from")
  else
    Try {
      if createDirs then os.makeDir.all(to / os.up)
      if os.isDir(from) then os.copy(from, to, replaceExisting = true, copyAttributes = true)
      else os.copy(from, to, replaceExisting = true)
    }.toEither.left.map(_.getMessage)

// --- Hilfsfunktionen ---

private def safePath(raw: String): Either[String, os.Path] =
  Try(os.Path(raw)).toEither.left.map(_.getMessage)

private def countOccurrences(content: String, target: String): Int =
  if target.isEmpty then 0
  else
    @scala.annotation.tailrec
    def loop(fromIndex: Int, acc: Int): Int =
      val idx = content.indexOf(target, fromIndex)
      if idx == -1 then acc
      else loop(idx + target.length, acc + 1)
    loop(0, 0)

private def formatSize(bytes: Long): String =
  if bytes < 1024 then s"${bytes} B"
  else if bytes < 1024 * 1024 then f"${bytes / 1024.0}%.1f KB"
  else if bytes < 1024 * 1024 * 1024 then f"${bytes / (1024.0 * 1024)}%.1f MB"
  else f"${bytes / (1024.0 * 1024 * 1024)}%.1f GB"

private def globToRegex(glob: String): String =
  def convert(chars: List[Char]): String = chars match
    case Nil             => ""
    case '*' :: '*' :: t => ".*" + convert(t)
    case '*' :: t        => "[^/]*" + convert(t)
    case '?' :: t        => "[^/]" + convert(t)
    case '.' :: t        => "\\." + convert(t)
    case c :: t          => scala.util.matching.Regex.quote(c.toString) + convert(t)
  s"^${convert(glob.toList)}$$"
