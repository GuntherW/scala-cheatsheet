### Run

```shell
cd cli/jfr &&  scala-cli run JfrMain1.scala --java-opt "-XX:StartFlightRecording=name=MinimalExample,settings=profile,dumponexit=true,filename=recording.jfr"
```

### See events

```shell
cd cli/jfr && jfr view all-events recording.jfr 
```