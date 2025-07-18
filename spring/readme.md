### Loom - Virtual Threads

```shell
docker run -p 9000:8080 mccutchen/go-httpbin
```

Um den Unterschied zwischen nativen und virtuellen Threads zu testen, in der application.properties das property
`spring.threads.virtual.enabled` togglen.

```shell
oha -c 30 -n 120 http://localhost:8093/delay/3
```