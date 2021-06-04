package de.codecentric.wittig.scala.threads

object Main extends App {

//  first()
  new Thread(task(1)).start()
  def first(): Unit = {
    println("a")

    /** * It calls the thread API of the underlying OS * Because the JVM uses a one to one mapping between Java and kernel threads, it asks the OS to give up the thread’s “rights” to the CPU for the
      * specified time * When the time has elapsed the OS scheduler will wake the thread via an interrupt (this is efficient) and assign it a CPU slice to allow it to resume running
      *
      * The critical point here is that the sleeping thread is completely taken out and is not reusable while sleeping.
      */
    Thread.sleep(10000)
    println("b")
  }

  def task(id: Int): Runnable = () => {
    println(s"${Thread.currentThread().getName()} start-$id")
    Thread.sleep(10000)
    println(s"${Thread.currentThread().getName()} end-$id")
  }

}
