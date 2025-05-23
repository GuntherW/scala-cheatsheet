package de.wittig.database.magnum

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import de.wittig.database.DatabaseName.MagnumDb
import de.wittig.database.dataSource

import scala.util.chaining.*

object WithJson extends App {

  private val xa = Transactor(dataSource(MagnumDb))

  connect(xa):
    sql"""
          drop table if exists people;
          create table if not exists people (
            id serial primary key ,
            metadata jsonb
          );
          """
      .update.run()

    // Mit diesem Index werden alle "@>" im "Where" abgedeckt.
    sql"""create index people_metadata_index on people using gin (metadata);""".update.run()

    // Extra Index, falls mit -> im Where gearbeitet wird.
    sql"""create index people_metadata_name_index on people using gin ((metadata->'name'));""".update.run()

    sql"""
         insert into people (metadata) values('{
              "name": "Gunther",
              "age": 23,
              "hobby": ["Fußball", "Scala"],
              "address": {
                "country":"Deutschland",
                "city":"Köln"
              }
         }');
         """.update.run()
    sql"""
         insert into people (metadata) values('{
              "name": "Peter",
              "age": 24,
              "hobby": ["Fußball"],
              "address": {
                "country":"Deutschland",
                "city":"Hamburg"
              }
         }');
         """.update.run()
    sql"""
         insert into people (metadata) values('{
              "name": "Hans",
              "age": 27,
              "hobby": [],
              "address": {
                "country":"Deutschland"
              }
         }');
         """.update.run()

    sql"""select metadata->'name' from people""".query[String].run().tap(println)
    sql"""select (metadata->'age')::int from people""".query[Int].run().tap(println)
    sql"""select metadata->'hobby'->0 from people""".query[String].run().tap(println)
    sql"""select metadata->'name', metadata->'hobby' from people""".query[(String, String)].run().tap(println)
//    sql"""select metadata->'name' from people where metadata->'hobby' ? 'Scala'""".query[String].run().tap(println)
    // sql"""select metadata->'name' from people where metadata->'hobby' ?& '["Fußball", "Scala"]'""".query[String].run().tap(println)
    // sql"""select metadata->'name' from people where metadata->'hobby' ?| '["Fußball", "Scala"]'""".query[String].run().tap(println)
    sql"""select metadata->'name' from people where metadata @> '{"name": "Gunther"}'""".query[String].run().tap(println)
    sql"""select metadata->'name' from people where metadata @> '{"hobby": ["Fußball"]}'""".query[String].run().tap(println)
    // sql"""select metadata->'name' from people where metadata->'name' = 'Gunther'""".query[String].run().tap(println)
}
