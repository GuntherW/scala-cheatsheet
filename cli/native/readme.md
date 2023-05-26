### Package native app
```shell
scala-cli  --power package native.scala -f -o nativeApp
./nativeApp
```

### -–native-mode (oder using nativeMode)
* debug - default, fast compilation with a slower runtime
* release-fast - moderate compilation time with a faster runtime
* release-full - slow compilation time with the fastest runtime