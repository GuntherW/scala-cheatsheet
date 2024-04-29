# scala-cli-multi-module-demo

- https://raw.githubusercontent.com/wiringbits/scala-cli-multi-module-demo/main/README.md

## Run tests

- `scala-cli test common`

## Execute modules

- `scala-cli module-1/App.scala`
- `scala-cli module-2/App.scala`

## Package modules

- `cd module-1 && ./package.sh`
- `cd module-2 && ./package.sh`

**NOTE**: `package.sh` is a shorthand for `scala-cli --power package App.scala -f`

## Format code

- `scala-cli fmt`
