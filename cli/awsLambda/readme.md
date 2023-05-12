### Bauen und lokales Testen einer Lambdafunktion für AWS

https://blog.lambdaspot.dev/one-and-done-part-2-industrys-adoption-of-self-contained-jvm-applications

```bash
scala-cli --power package MyApp.scala --assembly --preamble=false
```

```bash
echo '{"name": "John Doe", "age": 44}' | sam local invoke --event - "HelloFunction"
```