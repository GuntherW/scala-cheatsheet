package de.codecentric.wittig.scala.dynamictyping

object test {

	import Dyn._
	
	val a = new A                             //> a  : de.codecentric.wittig.scala.dynamictyping.Dyn.A = de.codecentric.wittig.
                                                  //| scala.dynamictyping.Dyn$A@6e3c9e46
	a.method(5)                               //> res0: String = 5
	a.someOther(4)                            //> res1: Int = 16
	a.someOtherMethod(4)                      //> res2: Int = 8
}