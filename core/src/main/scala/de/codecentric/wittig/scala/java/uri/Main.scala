package de.codecentric.wittig.scala.java.uri

import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

object Main extends App {

  // Create a URI
  try
    val uri = new URI("http://www.javacodegeeks.com/")
    System.out.println("URI created: " + uri)
  catch
    case e: URISyntaxException => System.out.println("URI Syntax Error: " + e.getMessage)

  // Convert URI to URL
  try
    val uri = new URI("http://www.javacodegeeks.com/")
    val url = uri.toURL
    System.out.println("URL from URI: " + url)
  catch
    case e: MalformedURLException => System.out.println("Malformed URL: " + e.getMessage)

}
