addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"     % "2.3.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"      % "1.0.2")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"  % "1.9.0")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix" % "0.9.34")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"  % "0.6.2")
addSbtPlugin("io.gatling"       % "gatling-sbt"  % "4.1.2")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
