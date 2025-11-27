//> using dep "com.lihaoyi::cask:0.11.3"

object CaskExample extends cask.MainRoutes {

  @cask.get("/")
  def hello() = "Hello World!"

  @cask.post("/do-thing")
  def doThing(request: cask.Request) =
    request.text().reverse

  // variable path segment, e.g. HOST/user/lihaoyi
  @cask.get("/user/:userName")
  def getUserProfile(userName: String) =
    s"User $userName"

  // GET allowing arbitrary sub-paths, e.g. HOST/path/foo/bar/baz
  @cask.get("/path")
  def getSubpath(segments: cask.RemainingPathSegments) =
    s"Subpath ${segments.value}"

  // POST allowing arbitrary sub-paths, e.g. HOST/path/foo/bar/baz
  @cask.post("/path")
  def postArticleSubpath(segments: cask.RemainingPathSegments) =
    s"POST Subpath ${segments.value}"

  // Mandatory query param, e.g. HOST/article/foo?param=bar
  @cask.get("/article/:articleId")
  def getArticle(articleId: Int, param: String) =
    s"Article $articleId $param"

  @cask.get("/article2/:articleId") // Optional query param
  def getArticleOptional(articleId: Int, param: Option[String] = None) =
    s"Article $articleId $param"

  // Optional query param with default
  @cask.get("/article3/:articleId")
  def getArticleDefault(articleId: Int, param: String = "DEFAULT VALUE") =
    s"Article $articleId $param"

  // 1-or-more param, e.g. HOST/article/foo?param=bar&param=qux
  @cask.get("/article4/:articleId")
  def getArticleSeq(articleId: Int, param: Seq[String]) =
    s"Article $articleId $param"

  initialize()
}
