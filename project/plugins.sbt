addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"     % "2.3.6")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"      % "1.0.2")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"  % "1.13.0")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix" % "0.10.4")
addSbtPlugin("io.gatling"       % "gatling-sbt"  % "4.2.6")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
