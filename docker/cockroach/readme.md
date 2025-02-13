### use sql shell
```shell
docker exec -it roach1 ./cockroach sql --insecure
```

### Create example database movr and fill it with example data
```shell
docker exec -it roach1 ./cockroach workload init movr 'postgresql://root@localhost:26257?sslmode=disable'
```
