addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"     % "2.3.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"      % "1.0.2")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"  % "1.11.0")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix" % "0.10.1")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"  % "0.6.3")
addSbtPlugin("io.gatling"       % "gatling-sbt"  % "4.2.4")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
