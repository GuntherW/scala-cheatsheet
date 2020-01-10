# mdoc 
Typechecked Markdown documentation for scala: https://scalameta.org/mdoc/


## Example for using a Variable "@VERSION@"
To __install__ _my_ project
```scala
libraryDependencies += "com" % "lib" % "@VERSION@"
```

## Using Scala Mdoc
### Erste Gehversuche mit mdoc

```scala mdoc
val length = 4
val multiplikator = 3
List.tabulate(length)(_ * multiplikator)
```

### Blockübergreifende Variablennutzung möglich

Hier wird `s1` definiert:
```scala mdoc
val s1 = Seq(1,2,3)
```

Später dann, kann auf `s1` zurückgegriffen werden:
```scala mdoc
val s2 = s1.map(_.toString)
```


## How to create Documentation
>1. create dir "docs"
>2. create a docs/readme.md
>3. add mdoc plugin to project/plugin.sbt
>4. configure build.sbt for mdoc
>5. in sbt, call `docs/mdoc` (use `--watch` for instant compilation)
>6. see result in configured output-folder (cheatsheet-docs/target/mdoc/readme.md)
