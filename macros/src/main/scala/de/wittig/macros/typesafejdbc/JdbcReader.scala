package de.wittig.macros.typesafejdbc

trait JdbcReader[A] { self =>
  def toOption: JdbcReader[Option[A]] = (value: Any) => Option(self.read(value))
  def toList: JdbcReader[List[A]]     = (value: Any) =>
    value.asInstanceOf[Array[Any]]
      .toList
      .map(self.read)
  def read(value: Any): A
}

object JdbcReader:
  def int: JdbcReader[Int]         = (value: Any) => value.asInstanceOf[Int]
  def string: JdbcReader[String]   = (value: Any) => value.asInstanceOf[String]
  def double: JdbcReader[Double]   = (value: Any) => value.asInstanceOf[Double]
  def boolean: JdbcReader[Boolean] = (value: Any) => value.asInstanceOf[Boolean]
