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