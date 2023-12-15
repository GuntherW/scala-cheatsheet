package de.wittig.cdk

import software.amazon.awscdk.services.s3.{Bucket, BucketProps}
import software.amazon.awscdk.{Stack, StackProps}
import software.constructs.Construct

class ConstructStackL2(scope: Construct, id: String, props: StackProps) extends Stack(scope, id, props) {
  def this(scope: Construct, id: String) = this(scope, id, null)

  new Bucket(
    this,
    "BucketStackL2",
    BucketProps.builder.bucketName("l2example-gunther-112233")
      .versioned(true)
      .publicReadAccess(false)
      .build
  )
}
