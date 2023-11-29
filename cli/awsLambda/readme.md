### Bauen und lokales Testen einer Lambdafunktion für AWS

https://blog.lambdaspot.dev/one-and-done-part-2-industrys-adoption-of-self-contained-jvm-applications

SAM = Serverless Application Model. (Wrapper für Cloudformation)

```bash


```bash
scala-cli --power package MyApp.scala -f --assembly --preamble=false
```

```bash
echo '{"name": "Daniel Düsentrieb", "age": 44}' | sam local invoke --event - "HelloFunction" | jq
```