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

## cdk subproject

The `cdk.json` file tells the CDK Toolkit how to execute your app.

### Useful commands

* `cdk ls`          list all stacks in the app
* `cdk synth`       emits the synthesized CloudFormation template
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk docs`        open CDK documentation

Enjoy!

### Zunächst muß CDKToolkit in der AWS deployt werden.
```shell
cdk bootstrap --profile ccGunther
```

### In den lokalen cdk.out Ordner. Hier kann nochmal überprüft werden, was cdk so an Cloudformation yamls erzeugt.
```shell
cdk synth --profile ccGunther
```

### Deployen in die AWS
```shell
cdk deploy --profile ccGunther
```