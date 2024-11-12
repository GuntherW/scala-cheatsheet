package de.codecentric.wittig.scala.free.monad

import scala.collection.mutable

/** @see
  *   https://www.youtube.com/watch?v=lzlCjgRWPDU
  * @see
  *   https://blog.rockthejvm.com/free-monad/
  */
object FreeMonad extends App:

  trait Monad[M[_]]:
    def pure[A](a: A): M[A]
    def flatMap[A, B](ma: M[A])(f: A => M[B]): M[B]

  object Monad:
    def apply[M[_]](using monad: Monad[M]): Monad[M] = monad

  // Natural Transformation
  trait ~>[F[_], G[_]]:
    def apply[A](fa: F[A]): G[A]

  trait Free[M[_], A]:
    import Free.*
    def flatMap[B](f: A => Free[M, B]): Free[M, B]   = FlatMap(this, f)
    def map[B](f: A => B): Free[M, B]                = flatMap(a => pure(f(a)))
    def foldMap[G[_]: Monad](natTrans: M ~> G): G[A] = this match
      case Pure(a)        => Monad[G].pure(a)
      case FlatMap(fa, f) => Monad[G].flatMap(fa.foldMap(natTrans))(a => f(a).foldMap(natTrans))
      case Suspend(ma)    => natTrans.apply(ma)

  object Free:
    def pure[M[_], A](a: A): Free[M, A]      = Pure(a)
    def liftM[M[_], A](ma: M[A]): Free[M, A] = Suspend(ma)

    case class Pure[M[_], A](a: A)                                     extends Free[M, A]
    case class FlatMap[M[_], A, B](fa: Free[M, A], f: A => Free[M, B]) extends Free[M, B]
    case class Suspend[M[_], A](ma: M[A])                              extends Free[M, A]

  /** Example for using Free Monad */
  trait DBOps[A]
  case class Create[A](key: String, value: A) extends DBOps[Unit]
  case class Read[A](key: String)             extends DBOps[A]
  case class Update[A](key: String, value: A) extends DBOps[A]
  case class Delete(key: String)              extends DBOps[Unit]

  // definition
  type DBMonad[A] = Free[DBOps, A]

  // "smart" constructors
  def create[A](key: String, value: A): DBMonad[Unit] = Free.liftM[DBOps, Unit](Create(key, value))
  def read[A](key: String): DBMonad[A]                = Free.liftM[DBOps, A](Read[A](key))
  def update[A](key: String, value: A): DBMonad[A]    = Free.liftM[DBOps, A](Update[A](key, value))
  def delete(key: String): DBMonad[Unit]              = Free.liftM[DBOps, Unit](Delete(key))

  // description of a computation
  def myLittleProgramm: DBMonad[Unit] =
    for
      _    <- create[String]("123", "Hallo")
      name <- read[String]("123")
      _    <- create[String]("xxx", "Welt")
      _    <- delete("123")
    yield ()

  // interpreter (evaluate the programm)
  case class IO[A](unsafeRun: () => A)
  object IO:
    def create[A](a: => A): IO[A] = IO(() => a)

  given ioMonad: Monad[IO] = new Monad[IO]:
    override def pure[A](a: A)                           = IO(() => a)
    override def flatMap[A, B](ma: IO[A])(f: A => IO[B]) = IO(() => f(ma.unsafeRun()).unsafeRun())

  val myDB: mutable.Map[String, String] = mutable.Map()
  def serialize[A](a: A): String        = a.toString
  def deserialize[A](value: String): A  = value.asInstanceOf[A]

  val dbOps2IO: DBOps ~> IO = new (DBOps ~> IO):
    override def apply[A](fa: DBOps[A]): IO[A] =
      fa match
        case Create(key, value) => IO.create {
            println(s"inserts into table(id, name) values($key, $value)")
            myDB += (key -> serialize(value))
            ()
          }
        case Read(key)          => IO.create {
            println(s"select from table(id, name) where id=$key")
            deserialize(myDB(key))
          }
        case Update(key, value) => IO.create {
            println(s"update table(name=$value) where id=$key")
            val oldValue = myDB(key)
            myDB += (key -> serialize(value))
            deserialize(oldValue)
          }
        case Delete(key)        => IO.create {
            println(s"delete from table where id=$key")
            myDB -= key
            ()
          }

  myLittleProgramm
    .foldMap(dbOps2IO) // IO[Unit]
    .unsafeRun()
  myDB.foreach(println)
