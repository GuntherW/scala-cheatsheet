package de.wittig.datatransformation.ducktape

import io.github.arainko.ducktape.*

@main
def mainDeep(): Unit =

  case class SourceToplevel1(level1: Option[SourceLevel1])
  case class SourceLevel1(level2: Option[SourceLevel2])
  case class SourceLevel2(level3: SourceLevel3)
  case class SourceLevel3(int: Int)

  case class DestToplevel1(level1: Option[DestLevel1])
  case class DestLevel1(level2: Option[DestLevel2])
  case class DestLevel2(level3: Option[DestLevel3])
  case class DestLevel3(int: Long)

  val source   = SourceToplevel1(Some(SourceLevel1(Some(SourceLevel2(SourceLevel3(1))))))
  val expected = DestToplevel1(Some(DestLevel1(Some(DestLevel2(Some(DestLevel3(11)))))))

  val dest =
    source.into[DestToplevel1]
      .transform(Field.computedDeep(_.level1.element.level2.element.level3.element.int, (int: Int) => int.toLong + 10))

  assert(dest == expected)
