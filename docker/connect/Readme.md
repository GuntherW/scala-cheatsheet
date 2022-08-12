### Kafka Connect

1. `curl http://localhost:8083/connector-plugins`
Hier muß jetzt auch ein "io.confluent.connect.jdbc.JdbcSourceConnector" zu finden sein.
2. docker-compose exec kafka-connect bash -c 'curl -i -X POST -H "Accept:application/json" \
   -H  "Content-Type:application/json" http://localhost:8083/connectors/ \
   -d @/connect/postgres-source.json'
3. `curl http://localhost:8083/connectors` Hier sollte jetzt jdbc_source_postgres_movies zu sehen sein
4. `curl http://localhost:8083/connectors/jdbc_source_postgres_movies`
5. `kafka-console-consumer --bootstrap-server=kafka:9092 --topic postgres-actors --from-beginning`


