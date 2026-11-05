package de.codecentric.wittig.scala.collection

import de.codecentric.wittig.scala.Printer.printlnBlue

/** Forgotten collection APIs
  */
@main
def main(): Unit =

  printlnBlue("transpose")
  transpose()

  printlnBlue("unfold")
  unfold()

  printlnBlue("scanleft")
  scanLeft()

  printlnBlue("slicing")
  slicing()

  printlnBlue("grouping")
  grouping()

  printlnBlue("combining")
  combining()

  printlnBlue("searching")
  searching()

  printlnBlue("sorting and aggregating")
  sortingAndAggregating()

  printlnBlue("modifying")
  modifying()

  printlnBlue("maps")
  maps()

  def transpose(): Unit =
    println(List(List(1, 2, 3), List(4, 5, 6)))
    println(List(List(1, 2, 3), List(4, 5, 6)).transpose)
    println(List(Set(1, 2, 3), Set(4, 5, 6)).transpose)
    println(List(Set(1, 2, 3), Set(4, 5, 6)).transpose.transpose)
    println(List("111000", "101010").transpose)

  def unfold(): Unit =
    val unfolded = List.unfold(0) { i =>
      if i < 10 then Some(i, i + 1) else None
    }
    println(unfolded)

    val fibonacci = Iterator.unfold((BigInt(0), BigInt(1))) {
      case (x, y) => Some((x, (y, x + y)))
    }
    println(fibonacci.take(10).toList)
    println(fibonacci.drop(90).next())

  def scanLeft(): Unit =
    val list    = List(1, 2, 3, 4, 5)
    val sumList = list.scanLeft(0)(_ + _)
    println(sumList)

  def slicing(): Unit =
    val list = List(1, 2, 3, 4, 5, 6, 7, 8)
    println(list.slice(2, 5))  // List(3, 4, 5)
    println(list.take(3))  // List(1, 2, 3)
    println(list.drop(3))  // List(4, 5, 6, 7, 8)
    println(list.takeRight(3))  // List(6, 7, 8)
    println(list.dropRight(3))  // List(1, 2, 3, 4, 5)
    println(list.takeWhile(_ < 4))  // List(1, 2, 3)
    println(list.dropWhile(_ < 4))  // List(4, 5, 6, 7, 8)
    println(list.splitAt(3))  // (List(1, 2, 3),List(4, 5, 6, 7, 8))
    println(list.span(_ < 4))  // (List(1, 2, 3),List(4, 5, 6, 7, 8))
    println(list.head)  // 1
    println(list.headOption)  // Some(1)
    println(list.tail)  // List(2, 3, 4, 5, 6, 7, 8)
    println(list.init)  // List(1, 2, 3, 4, 5, 6, 7)
    println(list.last)  // 8
    println(list.lastOption)  // Some(8)

  def grouping(): Unit =
    val list = List(1, 2, 3, 4, 5, 6, 7, 8)
    println(list.grouped(3).toList)  // List(List(1, 2, 3), List(4, 5, 6), List(7, 8))
    println(list.sliding(3).toList)  // List(List(1, 2, 3), List(2, 3, 4), ..., List(6, 7, 8))
    println(list.sliding(3, 2).toList)  // List(List(1, 2, 3), List(3, 4, 5), List(5, 6, 7), List(7, 8))
    println(list.groupBy(_ % 3))  // HashMap(0 -> List(3, 6), 1 -> List(1, 4, 7), 2 -> List(2, 5, 8))
    println(list.groupMap(_ % 3)(_ * 10))  // Map(0 -> List(30, 60), 1 -> List(10, 40, 70), 2 -> List(20, 50, 80))
    println(list.groupMapReduce(_ % 3)(identity)(_ + _))  // Map(0 -> 9, 1 -> 12, 2 -> 15)
    println(list.partition(_ % 2 == 0))  // (List(2, 4, 6, 8),List(1, 3, 5, 7))

  def combining(): Unit =
    val names = List("a", "b", "c")
    val nums  = List(1, 2, 3, 4)
    println(names.zip(nums))  // List((a,1), (b,2), (c,3))
    println(names.zipAll(nums, "-", 0))  // List((a,1), (b,2), (c,3), (-,4))
    println(names.zipWithIndex)  // List((a,0), (b,1), (c,2))
    println(names.zip(nums).unzip)  // (List(a, b, c),List(1, 2, 3))
    println(names.lazyZip(nums).map(_ + _))  // List(a1, b2, c3)
    println(names ++ List("d"))  // List(a, b, c, d)
    println("z" +: names :+ "d")  // List(z, a, b, c, d)
    println(names.flatMap(n => nums.take(2).map(n + _)))  // List(a1, a2, b1, b2, c1, c2)
    println(List(List(1), List(2, 3)).flatten)  // List(1, 2, 3)

  def searching(): Unit =
    val list = List(3, 1, 4, 1, 5, 9, 2, 6)
    println(list.find(_ > 4))  // Some(5)
    println(list.exists(_ > 8))  // true
    println(list.forall(_ > 0))  // true
    println(list.count(_ % 2 == 0))  // 3
    println(list.indexOf(1))  // 1
    println(list.lastIndexOf(1))  // 3
    println(list.indexWhere(_ > 4))  // 4
    println(list.contains(9))  // true
    println(list.collectFirst { case x if x > 4 => x * 10 })  // Some(50)
    println(list.collect { case x if x % 2 == 0 => x * 10 })  // List(40, 20, 60)

  def sortingAndAggregating(): Unit =
    val list = List(3, 1, 4, 1, 5, 9, 2, 6)
    println(list.sorted)  // List(1, 1, 2, 3, 4, 5, 6, 9)
    println(list.sorted(using Ordering[Int].reverse))  // List(9, 6, 5, 4, 3, 2, 1, 1)
    println(list.sortBy(-_))  // List(9, 6, 5, 4, 3, 2, 1, 1)
    println(list.sortWith(_ > _))  // List(9, 6, 5, 4, 3, 2, 1, 1)
    println(list.distinct)  // List(3, 1, 4, 5, 9, 2, 6)
    println(list.reverse)  // List(6, 2, 9, 5, 1, 4, 1, 3)
    println(list.min)  // 1
    println(list.maxBy(x => -x))  // 1
    println(list.sum)  // 31
    println(list.product)  // 6480
    println(list.foldLeft(0)(_ + _))  // 31
    println(list.reduce(_ max _))  // 9
    println(list.reduceOption(_ max _))  // Some(9)
    println(list.diff(List(1, 2)))  // List(3, 4, 1, 5, 9, 6)
    println(list.intersect(List(1, 2, 7)))  // List(1, 2)
    println(list.concat(List(7)))  // List(3, 1, 4, 1, 5, 9, 2, 6, 7)

  def modifying(): Unit =
    val list = List(1, 2, 3, 4, 5)
    println(list.updated(1, 20))  // List(1, 20, 3, 4, 5)
    println(list.patch(1, List(9, 9), 2))  // List(1, 9, 9, 4, 5)
    println(list.padTo(7, 0))  // List(1, 2, 3, 4, 5, 0, 0)
    println(list.filterNot(_ % 2 == 0))  // List(1, 3, 5)
    println(list.map(_ * 2))  // List(2, 4, 6, 8, 10)
    println(list.tapEach(_ => ()).mkString("[", ", ", "]"))  // [1, 2, 3, 4, 5]
    println(list.view.map(_ * 2).filter(_ > 4).toList)  // List(6, 8, 10)
    println(list.drop(2) ++ list.take(2))  // List(3, 4, 5, 1, 2)
    println(list.mkString(","))  // 1,2,3,4,5
    println(list.isEmpty)  // false
    println(list.nonEmpty)  // true
    println(list.size)  // 5
    println(list.lengthCompare(3))  // 1 (Länge ist größer als 3)
    println(list.inits.toList)  // List(List(1, 2, 3, 4, 5), List(1, 2, 3, 4), ..., List(1), List())
    println(list.tails.toList)  // List(List(1, 2, 3, 4, 5), List(2, 3, 4, 5), ..., List(5), List())
    println(list.combinations(2).toList)  // List(List(1, 2), List(1, 3), List(1, 4), ..., List(4, 5))
    println(list.permutations.take(3).toList)  // List(List(1, 2, 3, 4, 5), List(1, 2, 3, 5, 4), List(1, 2, 4, 3, 5))
    println(list.iterator.sliding(2).map(_.sum).toList)  // List(3, 5, 7, 9)

  def maps(): Unit =
    val map = Map("a" -> 1, "b" -> 2, "c" -> 3)
    println(map.get("a"))  // Some(1)
    println(map.getOrElse("z", 0))  // 0
    println(map.view.mapValues(_ * 10).toMap)  // Map(a -> 10, b -> 20, c -> 30)
    println(map.filter((_, v) => v > 1))  // Map(b -> 2, c -> 3)
    println(map.map((k, v) => k.toUpperCase -> v))  // Map(A -> 1, B -> 2, C -> 3)
    println(map.updated("d", 4))  // Map(a -> 1, b -> 2, c -> 3, d -> 4)
    println(map + ("e" -> 5))  // Map(a -> 1, b -> 2, c -> 3, e -> 5)
    println(map - "a")  // Map(b -> 2, c -> 3)
    println(map.keys)  // Set(a, b, c)
    println(map.values)  // Iterable(1, 2, 3)
    println(map.toList.sortBy(_._2))  // List((a,1), (b,2), (c,3))
    println(map.removed("b"))  // Map(a -> 1, c -> 3)
    println(map.foldLeft(0)((acc, kv) => acc + kv._2))  // 6
    println(map.withDefaultValue(0)("zzz"))  // 0
