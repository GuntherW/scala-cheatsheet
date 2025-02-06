package de.wittig.macros.rockthejvm.macros
import quoted.*

object MSymbols:

  inline def describeSymbols[A]: Unit = ${ describeSymbolsImpl[A] }

  private def describeSymbolsImpl[A: Type](using q: Quotes): Expr[Unit] =
    import q.reflect.*
    val symbol = TypeRepr.of[A].typeSymbol

    val firstMethod  = symbol.methodMember("changePermissions").head
    val bitMaskField = symbol.fieldMember("bitMask")

    println("symbol.name: " + symbol.name)
    println("symbol.fullName: " + symbol.fullName)
    println("symbol.companionModule: " + symbol.companionModule)
    println("symbol.methodMember params: " + firstMethod.paramSymss) // list of all param
    println("symbol.methodMember flags: " + firstMethod.flags.show)

    println(bitMaskField.pos)
    println("symbol.flags: " + symbol.flags)

    println("symbol.children: " + symbol.children(0).primaryConstructor.paramSymss)
//    println(symbol.tree)
    '{ () }
