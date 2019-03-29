package de.codecentric.wittig.scala.mockito

/**
  * @author gunther
  */
class ServiceUnterTest(val abh: AbhaengigerService) extends Service {

  override val andererService = new AbhaengigerService2 {}
  def aufruf: Int = {
    abh.auchAufruf + andererService.auchAufruf + 1
  }
}

trait Service {
  def aufruf: Int
  val andererService: AbhaengigerService2
}

trait AbhaengigerService {
  def auchAufruf: Int = 1
}
trait AbhaengigerService2 {
  def auchAufruf: Int = 1
}
