### Helper Types

+ `UIO[A]     = ZIO[Any, Nothing, A]  `: effect that has no requirements, and cannot fail, but can succeed with an A. UIU = Unfailable IO
+ `IO[E, A]   = ZIO[Any, E, A]        `: effect that has no requirements, and may fail with an E, or succeed with an A.
+ `URIO[R, A] = ZIO[R, Nothing, A]    `: effect that requires an R, and cannot fail, but can succeed with an A.
+ `Task[A]    = ZIO[Any, Throwable, A]`: effect that has no requirements, and may fail with a Throwable value, or succeed with an A.
+ `RIO[R, A]  = ZIO[R, Throwable, A]  `: effect that requires an R, and may fail with a Throwable value, or succeed with an A.
