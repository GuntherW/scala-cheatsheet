# scala-cheatsheet

[![Actions Status](https://github.com/GuntherW/scala-cheatsheet/workflows/Scala%20CI/badge.svg)](https://github.com/GuntherW/scala-cheatsheet/actions)
+ Just a collection of some interesting scala stuff.
+ Playground.

### ScalaJs
Run
```
npm init private
npm install jsdom
```


### Scala3 Migration

#### 1. Schritt
migrate-libs munit
migrate-syntax munit

#### 2. Schritt
Add Compiler Flags zu Projekt in build.sbt

```scalacOptions ++= Seq("-indent", "-rewrite"),```

Danach ???:

```scalacOptions ++= Seq("-indent", "-new-syntax"),```