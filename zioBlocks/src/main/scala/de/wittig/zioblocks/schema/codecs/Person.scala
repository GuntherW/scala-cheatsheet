package de.wittig.zioblocks.schema.codecs

import zio.blocks.schema.Schema

import java.time.LocalDate

case class Person(name: String, age: Int, date: LocalDate = LocalDate.now) derives Schema
