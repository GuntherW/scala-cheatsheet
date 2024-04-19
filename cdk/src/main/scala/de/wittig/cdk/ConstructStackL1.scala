package de.wittig.cdk

import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.s3.CfnBucket
import software.amazon.awscdk.services.s3.CfnBucketProps
import software.constructs.Construct

class ConstructStackL1(scope: Construct, id: String, props: StackProps) extends Stack(scope, id, props) {
  def this(scope: Construct, id: String) = this(scope, id, null)

  new CfnBucket(
    this,
    "BucketStackL1",
    CfnBucketProps.builder.bucketName("l1example-gunther-112233")
      .versioningConfiguration(
        CfnBucket.VersioningConfigurationProperty.builder
          .status("Enabled")
          .build
      )
      .publicAccessBlockConfiguration(
        CfnBucket.PublicAccessBlockConfigurationProperty.builder
          .blockPublicAcls(true)
          .blockPublicPolicy(true)
          .ignorePublicAcls(true)
          .restrictPublicBuckets(true)
          .build
      ).build
  )
}
