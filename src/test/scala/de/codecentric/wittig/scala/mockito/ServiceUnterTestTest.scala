package de.codecentric.wittig.scala.mockito
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

/**
 * @author gunther
 */
class ServiceUnterTestTest extends FunSuite with MockitoSugar {

  test("ServiceUnterTest") {

    //Mocken
    val m = mock[AbhaengigerService]
    val m2 = mock[AbhaengigerService2]

    //Bedingungen
    when(m.auchAufruf).thenReturn(2)
    when(m2.auchAufruf).thenReturn(3)

    //SUT
    val service = new ServiceUnterTest(m) {
      override val andererService = m2
    }

    assert(service.aufruf == 6)

    //Verify
    verify(m, times(1)).auchAufruf
    verify(m2, times(1)).auchAufruf

  }
}
