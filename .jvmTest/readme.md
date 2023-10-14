https://dzone.com/articles/best-practices-java-memory-arguments-for-container

java -XX:+PrintFlagsFinal -XX:MaxRAMPercentage=50 -version | grep -Ei "maxheapsize|maxram|initialheapsize"


```shell
docker build -t guntherjava .
```

```shell
docker run --rm -ti --memory=2g guntherjava
```
```shell
docker run --rm -ti --memory=1g guntherjava
```
```shell
docker run --rm -ti guntherjava
```