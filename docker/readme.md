
### Starten eines Dockerfiles

```shell
docker run --rm -it $(docker build -q .)
```



## Multi Arch
### registere QEMU binaries

```shell
docker run --privileged --rm tonistiigi/binfmt --install all
```

```shell
docker buildx rm
```

```shell
docker buildx create --use --name multi-arch-builder
```

```shell
#docker build -t temurin-hello-app .
#docker buildx build -t temurin-hello-app-arm2 --platform linux/arm64 --load .
#docker buildx build -t temurin-hello-app-amd2 --platform linux/amd64 --load .
docker buildx build  --platform linux/amd64,linux/arm64 -t 123456789.dkr.ecr.eu-central-1.amazonaws.com/date-date:gunthertest --push .
```
