package de.wittig.macros.rockthejvm.macros
import quoted.*

object HBuildingExpressions:
  enum Permissions:
    case Denied
    case Bitset(value: Int)
    case Custom(dir: List[String])

  def buildStringExpr(using Quotes): Expr[String] =
    Expr("This is a String")

  inline def createDefaultPermissions: Permissions = ${ buildPermissionsExpr }

  // does not compile unless wie have a given ToExpr[Permissions]
  def buildPermissionsExpr(using Quotes): Expr[Permissions] =
    Expr(Permissions.Custom(List("eins", "zwei"))) // requires a given ToExpr[Permissions]

  // we have to provide a given ToExpr[Permissions]
  // we have to provide a given FromExpr[Permissions]
  import Permissions.*
  given ToExpr[Permissions] with
    def apply(p: Permissions)(using Quotes): Expr[Permissions] = p match
      case Denied        => '{ Denied }
      case Bitset(value) => '{ Bitset(${ Expr(value) }) }
      case Custom(dir)   => '{ Custom(${ Expr(dir) }) }

  given FromExpr[Permissions] with
    def unapply(expr: Expr[Permissions])(using Quotes): Option[Permissions] =
      expr match
        case '{ Denied }                   => Some(Denied)
        case '{ Bitset(${ Expr(value) }) } => Some(Bitset(value))
        case '{ Custom(${ Expr(dir) }) }   => Some(Custom(dir))
        case _                             => None

    //
    /** Able to use .valueOrAbort on Expr with a given FromExpr[Permissions] */
  inline def describePermissions(inline p: Permissions): String = ${ describePermissionsImpl('p) }

  private def describePermissionsImpl(p: Expr[Permissions])(using Quotes): Expr[String] =
    val perm   = p.valueOrAbort // can turn an Expr into a value if I have a given FromExpr[Permissions]
    val result = perm match
      case Denied        => "You have no permissions"
      case Bitset(value) => s"You have permissions $value"
      case Custom(dir)   => s"You have custom permissions: ${dir.mkString(", ")}"
    Expr(result)
