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

    //Bedingungen
    when(m.auchAufruf).thenReturn(2)

    //SUT
    val service = new ServiceUnterTest(m)

    assert(service.aufruf == 5)

    //Verify
    verify(m, times(2)).auchAufruf

  }
}
