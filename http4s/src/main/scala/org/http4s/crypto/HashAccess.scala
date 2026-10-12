package org.http4s.crypto

import cats.effect.IO

// Hash is private[http4s] in http4s-crypto, which Scala 3.10 enforces for implicit search
object HashAccess:
  given Hash[IO] = Hash.forApplicativeThrow[IO]
