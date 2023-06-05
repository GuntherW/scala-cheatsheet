### Package and Run (requires spark-submit in PATH and SPARK_HOME to be set)

```bash
scala-cli --power package --spark SparkJob.scala -o spark-job.jar
spark-submit spark-job.jar
```

### Run (same as above) (requires spark-submit in PATH and SPARK_HOME to be set
```bash
scala-cli --power run --spark SparkJob.scala 
```

### Run standalone
```bash
scala-cli --power run --spark-standalone SparkJob.scala
```