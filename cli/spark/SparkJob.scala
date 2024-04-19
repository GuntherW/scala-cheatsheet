//> using dep org.apache.spark::spark-sql:3.5.1
//> using scala 2.13.11

import org.apache.spark._
import org.apache.spark.sql._

object SparkJob extends App {

  val spark = SparkSession.builder().appName("Test job").getOrCreate()
  import spark.implicits._
  def sc    = spark.sparkContext

  val accum = sc.longAccumulator
  sc
    .parallelize(1 to 10)
    .foreach(x => accum.add(x))
  println("Result: " + accum.value)
}
