### use sql shell
```shell
docker exec -it roach1 ./cockroach sql --insecure
```

### Create example database movr and fill it with example data
```shell
docker exec -it roach1 ./cockroach workload init movr 'postgresql://root@localhost:26257?sslmode=disable'
```

### Migration
#### Create DB
```shell
createdb -h localhost -U postgres frenchtowns
```
#### Import Data
```shell
psql -h localhost -U postgres frenchtowns -a -f frenchtowns.sql
```

```shell
pg_dump -h localhost -U postgres --schema-only frenchtowns > frenchtowns_schema.sql
```
### jvBUeR3SbtoaYVMJJyZjAQ gunther
### krAiNMRfjBZTe8gNfBPe4w postgres

```Java
PGSimpleDataSource ds = new PGSimpleDataSource();
ds.setUrl("jdbc:postgresql://cockroach-c-1-8086.j77.cockroachlabs.cloud:26257/defaultdb?sslmode=verify-full");
ds.setUser("gunther")
ds.setPassword("jvBUeR3SbtoaYVMJJyZjAQ")
```

```shell
export JDBC_DATABASE_URL="jdbc:postgresql://cockroach-c-1-8086.j77.cockroachlabs.cloud:26257/defaultdb?sslmode=verify-full&password=jvBUeR3SbtoaYVMJJyZjAQ&user=gunther"
```