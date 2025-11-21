package de.wittig.json.avro
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema}
import org.apache.avro.Schema

import java.io.File

@main
def main(): Unit =

  case class Ingredient(name: String, sugar: Double, fat: Double)
  case class Pizza(name: String, ingredients: Seq[Ingredient], vegetarian: Boolean, vegan: Boolean, calories: Int)
  object Pizza:
    val schema: Schema = AvroSchema[Pizza]

  val pepperoni = Pizza("pepperoni", Seq(Ingredient("pepperoni", 12, 4.5), Ingredient("onions", 1, 0.4)), false, false, 598)
  val hawaiian  = Pizza("hawaiian", Seq(Ingredient("ham", 1.5, 5.6), Ingredient("pineapple", 5.2, 0.2)), false, false, 391)

  // serialize
  val os = AvroOutputStream.data[Pizza].to(new File("json/pizzas.avro")).build()
  os.write(Seq(pepperoni, hawaiian))
  os.flush()
  os.close()

  // deserialize
  val is     = AvroInputStream.data[Pizza].from(new File("json/pizzas.avro")).build(Pizza.schema)
  val pizzas = is.iterator.toSet
  is.close()

  pizzas.foreach(println)
