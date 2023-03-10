### build

```shell
sdk use java 22.3.r11-grl
gu install native-image
sudo apt install build-essential
sudo apt install libz-dev
```


```shell
sbt graalvm-native-image:packageBin
```