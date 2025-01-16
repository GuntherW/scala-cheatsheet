Hochfahren der Datenbank

```shell
cd ../docker
docker compose down
docker compose build 
```


```shell
cd ../docker
docker compose up postgres -d
```

```shell
pgcli -h localhost -p 5433 -u postgres -d persondb
```

```sql
show TRANSACTION ISOLATION LEVEL
```