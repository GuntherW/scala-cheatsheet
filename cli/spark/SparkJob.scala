//> using dep org.apache.spark::spark-sql:4.2.0
//> using scala 2.13.18

import org.apache.spark._
import org.apache.spark.sql._

object SparkJob {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Test job")
      .master("local[*]")
      .getOrCreate()
    import spark.implicits._
    val sc = spark.sparkContext

    val accum = sc.longAccumulator
    sc
      .parallelize(1 to 10)
      .foreach(x => accum.add(x))
    println("Result: " + accum.value)
    spark.stop()
  }
}
