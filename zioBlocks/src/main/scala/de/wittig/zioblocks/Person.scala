package de.wittig.zioblocks

import zio.blocks.schema.Schema

case class Person(name: String, age: Int) derives Schema
