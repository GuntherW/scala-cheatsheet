### Start Replica 1 (in einem eigenen Terminal)
```shell
sbt http4s/runMain de.wittig.http4s.loadbalancer.Replica 7071
```
### Start Replica 2 (in einem eigenen Terminal)
```shell
sbt http4s/runMain de.wittig.http4s.loadbalancer.Replica 7072
```
### Start Replica 3 (in einem eigenen Terminal)
```shell
sbt http4s/runMain de.wittig.http4s.loadbalancer.Replica 7073
```

### Start Loadbalancer (in einem eigenen Terminal)
```shell
sbt http4s/runMain de.wittig.http4s.loadbalancer.Server
```

### Test Replica 1
```shell
http localhost:7070/hallo/welt
http localhost:7070/hallo/welt
http localhost:7070/hallo/welt
http localhost:7070/hallo/welt
```