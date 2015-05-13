package de.codecentric.wittig.scala.dynamictyping

object test {

	import Dyn._;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(96); 
	
	val a = new A;System.out.println("""a  : de.codecentric.wittig.scala.dynamictyping.Dyn.A = """ + $show(a ));$skip(13); val res$0 = 
	a.method(5);System.out.println("""res0: String = """ + $show(res$0));$skip(16); val res$1 = 
	a.someOther(4);System.out.println("""res1: Int = """ + $show(res$1));$skip(22); val res$2 = 
	a.someOtherMethod(4);System.out.println("""res2: Int = """ + $show(res$2))}
}
