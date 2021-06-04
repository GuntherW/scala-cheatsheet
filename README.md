zure# scala-cheatsheet

[![Build Status](https://travis-ci.org/GuntherW/scala-cheatsheet.svg)](https://travis-ci.org/GuntherW/scala-cheatsheet)

+ Just a collection of some interesting scala stuff.
+ Playground.

### Scala3 Migration

#### 1. Schritt
migrate-libs munit
migrate-syntax munit

#### 2. Schritt
Add Compiler Flags zu Projekt in build.sbt

```scalacOptions ++= Seq("-indent", "-rewrite"),```

Danach ???:

```scalacOptions ++= Seq("-indent", "-new-syntax"),```