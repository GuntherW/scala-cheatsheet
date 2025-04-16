package de.wittig.sttp

import sttp.capabilities.Effect
import sttp.client4.*
import sttp.client4.wrappers.DelegateBackend
import sttp.model.Header

// If some headers should be added to each request, the best approach is to create a request description which includes
// these headers. Then, specific requests can use such a request template, just as `basicRequest` is used to create
// requests normally (it already includes some a default `Accept-Encoding` header). However, if the request description
// is for any reason outside the control of the user, a backend wrapper can be used to add headers to each request.
class AddHeaderBackendWrapper[F[_], P](delegate: GenericBackend[F, P], headers: List[Header]) extends DelegateBackend(delegate):
  override def send[T](request: GenericRequest[T, P with Effect[F]]): F[Response[T]] =
    delegate.send(headers.foldLeft(request) { case (r, h) => r.header(h) })

object AddHeaderBackendWrapper:
  def apply(backend: WebSocketSyncBackend, headers: List[Header]): WebSocketSyncBackend =
    new AddHeaderBackendWrapper(backend, headers) with WebSocketSyncBackend {}

// depending on the backend & effect type used, other "apply" variants might be needed here
object AddHeaderToAllRequests extends App:

  val backend: WebSocketSyncBackend = AddHeaderBackendWrapper(
    DefaultSyncBackend(),
    List(Header("X-My-Header1", "value1"), Header("X-My-Header2", "value2"))
  )

  val response: Response[Either[String, String]] = basicRequest.get(uri"https://httpbin.org/get").send(backend)

  println("Response:")
  println(response.body)
