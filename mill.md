```shell
mill __.compile
```

```shell
mill __.test
```

```shell
mill mill.scalalib.Dependency/showUpdates
```

### Macro Output in Console:

```shell
mill -w macros.compile
```

### Run JMH:

```shell
mill core.runJmh
mill core.runJmh Treiber      # Run all benchmarks matching regex `Treiber`
```
