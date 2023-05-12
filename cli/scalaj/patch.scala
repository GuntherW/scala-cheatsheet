//> using jvm "17"
//> using scala "2.13"
//> using dep "org.scalaj::scalaj-http::2.4.2"

import scalaj.http._

object Main extends App {

  lazy val patch: HttpRequest =
     Http("https://httpbin.org/patch")
    .postData("Hallo")
    .header("Accept", "application/json")
    .method("patch")
  lazy val get: HttpRequest =
     Http("https://httpbin.org/get")
    .header("Accept", "application/json")
    .method("get")
    
  println(get.asString)
  println(patch.asString)
}
