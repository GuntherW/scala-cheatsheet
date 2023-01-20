### Schritte

```shell 
scala-cli setup-ide .
scala-cli package StringUtil.scala -o string-util-native -f

./string-util-native join -s , eins zwei drei
./string-util-native split -s , eins,zwei,drei
```