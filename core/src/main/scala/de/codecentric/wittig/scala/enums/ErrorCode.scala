package de.axa.transactional.bodyguard.model

/**
  * @author gunther
  */
sealed trait ErrorCode { def code: Int }

object ErrorCode {
  case object UserNotExists    extends ErrorCode { val code = 1 }
  case object TwilioError      extends ErrorCode { val code = 2 }
  case object ParameterMissing extends ErrorCode { val code = 3 }
  case object InvalidJson      extends ErrorCode { val code = 4 }
  case object DbError          extends ErrorCode { val code = 5 }

  val all: List[ErrorCode] = List(
    UserNotExists,
    TwilioError,
    ParameterMissing,
    InvalidJson,
    DbError
  ).sortBy(x => x.code)
}
