### Run
```shell
scala-cli setup-ide .
scala-cli demo.scala --revolver
scala-cli scripting --main-class demo_sc
scala-cli js.scala
scala-cli native.scala
scala-cli https://gist.github.com/GuntherW/765efaeaaaa55d43e91f23dc6795e462
```

### Update all
```shell
scala-cli dependency-update . --all
```

### Package js app
```shell
scala-cli package js.scala
node js.js
```

### Run as script
```shell
chmod +x embeddable.scala
./embeddable.scala
```

