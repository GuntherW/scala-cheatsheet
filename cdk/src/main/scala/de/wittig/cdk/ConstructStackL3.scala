package de.wittig.cdk

import software.amazon.awscdk.services.ec2.{Vpc, VpcProps}
import software.amazon.awscdk.services.ecs.patterns.{ApplicationLoadBalancedFargateService, ApplicationLoadBalancedFargateServiceProps, ApplicationLoadBalancedTaskImageOptions}
import software.amazon.awscdk.services.ecs.{Cluster, ClusterProps, ContainerImage}
import software.amazon.awscdk.{Stack, StackProps}
import software.constructs.Construct

class ConstructStackL3(scope: Construct, id: String, props: StackProps) extends Stack(scope, id, props) {
  def this(scope: Construct, id: String) = this(scope, id, null)

  private val vpc     = new Vpc(this, "VpcStackL3", VpcProps.builder.maxAzs(2).build)
  private val cluster = new Cluster(this, "ClusterStackL3", ClusterProps.builder.vpc(vpc).build)

  new ApplicationLoadBalancedFargateService(
    this,
    "ConstructFargateServiceL3",
    ApplicationLoadBalancedFargateServiceProps.builder.cluster(cluster)
      .publicLoadBalancer(true)
      .taskImageOptions(
        ApplicationLoadBalancedTaskImageOptions.builder
          .image(ContainerImage.fromRegistry("amazon/amazon-ecs-sample"))
          .build
      ).memoryLimitMiB(512)
      .desiredCount(2)
      .build
  )
}
