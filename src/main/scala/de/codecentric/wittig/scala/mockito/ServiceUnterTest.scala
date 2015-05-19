package de.codecentric.wittig.scala.mockito

/**
 * @author gunther
 */
class ServiceUnterTest(val abh: AbhaengigerService) extends Service {

  def aufruf: Int = {
    abh.auchAufruf + abh.auchAufruf + 1
  }
}

trait Service {
  def aufruf: Int
}

trait AbhaengigerService {
  def auchAufruf: Int = 1
}
