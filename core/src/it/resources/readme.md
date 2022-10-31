### How to integration test:

1. Anlegen eines `it` Ordners in `src`
2. Konfiguration von Sbt:
    ```
    lazy val root = (project in file("."))
         .configs(IntegrationTest)
         .settings(
            Defaults.itSettings,
         )
    ```
3. Bereitstellen der Bibliotheken für `it` in der `build.sbt`:

> `libraryDependencies += scalatest % "it,test"`

### Links

https://iamninad.com/organizing-scala-tests-for-faster-feedback/