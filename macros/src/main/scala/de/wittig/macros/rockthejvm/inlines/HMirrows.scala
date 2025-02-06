package de.wittig.macros.rockthejvm.inlines
import tools.*
import scala.deriving.Mirror
import compiletime.*
import de.wittig.macros.rockthejvm.inlines.tools.Show.show

// derives: compiler will look for a method "derived" in the companion object of Show
// it will return a Show[Person]
// derives Show will give us a GIVEN Show[Person] instance
case class Person(name: String, age: Int, programmer: Boolean) derives Show

enum Permissions:
  case READ, WRITE, EXECUTE

// Automatically derive Show[A] where A can be any Sum Type or Product Type
// mirror contains all type information at compile time
object Mirrows extends App:

  /** Mirror of Product Type */
  val personMirror                   = summon[Mirror.Of[Person]]                        // Mirror.ProductOf[Person]
  val gunther: Person                = personMirror.fromTuple(("Gunther", 123, true))
  val aTuple: (String, Int, Boolean) = Tuple.fromProductTyped(gunther)
  val className                      = constValue[personMirror.MirroredLabel]           // the name of the class, known at compile time
  val fieldNames                     = constValueTuple[personMirror.MirroredElemLabels] // the names of the fields, known at compile time

  /** Mirror of Sum Type */
  val permissionsMirror    = summon[Mirror.Of[Permissions]] // Mirror.SumOf[Permissions]
  val classNamePermissions = constValue[permissionsMirror.MirroredLabel]
  val allCases             = constValueTuple[permissionsMirror.MirroredElemLabels]

  val masterYoda  = Person("Master Yoda", 900, programmer = false)
  val showPerson1 = Show.derived[Person] // explicit call
  val showPerson2 = summon[Show[Person]] // implicit call
  val showYoda    = showPerson2.show(masterYoda)

  println(showYoda)
  println(masterYoda.show)
