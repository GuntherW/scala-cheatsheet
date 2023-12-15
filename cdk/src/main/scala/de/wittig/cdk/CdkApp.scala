package de.wittig.cdk

import software.amazon.awscdk.{App, StackProps}

object CdkApp extends scala.App {

  private val app = new App
  new ConstructStackL1(app, "ConstructStackL1", StackProps.builder.stackName("L1Example").build)
  new ConstructStackL2(app, "ConstructStackL2", StackProps.builder.stackName("L2Example").build)
  new ConstructStackL3(app, "ConstructStackL3", StackProps.builder.stackName("L3Example").build)

  app.synth
}
