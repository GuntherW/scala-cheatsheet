### Run
```
scala-cli demo.scala
scala-cli js.scala
scala-cli native.scala
scala-cli https://gist.github.com/GuntherW/765efaeaaaa55d43e91f23dc6795e462
```

### Package js app
```
scala-cli package js.scala
node js.js
```

### Package native app
```
scala-cli package native.scala -o nativeApp
./nativeApp
```

### Run as script
```
chmod +x embeddable.scala
./embeddable.scala
```

