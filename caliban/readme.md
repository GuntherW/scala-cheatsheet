
### Generate Client
sbt
cd caliban
sbt:caliban> calibanGenClient caliban/schema.graphql caliban/src/main/scala/de/wittig/client/generated/Client.scala

oder falls Server schon gestartet
sbt:caliban> calibanGenClient http://localhost:8088/api/graphql caliban/src/main/scala/de/wittig/client/generated/Client.scala

