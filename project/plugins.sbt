addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"     % "2.2.18")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"      % "1.0.0")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"  % "1.5.0")
addSbtPlugin("ch.epfl.lamp"     % "sbt-dotty"    % "0.5.3")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
