### Run

```shell
scala-cli run JfrMain1.scala --java-opt "-XX:StartFlightRecording=name=MinimalExample,settings=profile,dumponexit=true,filename=recording.jfr"
```

### See events
```shell
jfr view all-events recording.jfr 
```