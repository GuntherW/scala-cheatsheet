package de.wittig.database.magnum

import java.time.OffsetDateTime

import scala.concurrent.duration.DurationInt

import com.augustnagro.magnum.*
import de.wittig.database.dataSource
import de.wittig.database.DatabaseName.MagnumDb

object MainWithRepo extends App {

  private val xa       = Transactor(dataSource(MagnumDb), sqlLogger = SqlLogger.logSlowQueries(3.milliseconds))
  private val userRepo = UserRepo()

  val count = connect(xa):
    userRepo.count
  println(count)

  def withSpecifications = {
    // Specifications:
    val partialEmail            = "a"
    val nameOpt: Option[String] = None
    val searchDate              = OffsetDateTime.now.minusDays(2)
    val idPosition              = 42L

    // TODO: Tuts nicht, wenn ich die anderen Klauseln auch aktiviere.
    val spec = Spec[Person]
      .where(sql"email ILIKE 'a%'")
      //    .where(nameOpt.map(ln => sql"name = $ln").getOrElse(sql""))
      //    .where(sql"created >= $searchDate")
      //    .seek("id", SeekDir.Gt, idPosition, SortOrder.Asc)
      .limit(10)

    connect(xa):
      val users: Vector[Person] = userRepo.findAll(spec)
      users.foreach(println)
  }
}
