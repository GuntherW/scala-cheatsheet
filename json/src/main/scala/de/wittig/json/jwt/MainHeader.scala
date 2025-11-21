package de.wittig.json.jwt

import pdi.jwt.{JwtAlgorithm, JwtHeader}

import scala.util.chaining.scalaUtilChainingOps

@main
def mainHeader(): Unit =

  JwtHeader()
  JwtHeader(JwtAlgorithm.HS256)
  JwtHeader(JwtAlgorithm.HS256, "JWT")

  // You can stringify it to JSON
  JwtHeader(JwtAlgorithm.HS256, "JWT").toJson.tap(println)

  // You can assign the default type (but it would have be done automatically anyway)
  JwtHeader(JwtAlgorithm.HS256).withType.tap(println)
