addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.4.2")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"           % "2.2.21")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"            % "1.0.1")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.6.0")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"       % "0.9.29")
addSbtPlugin("ch.epfl.scala"    % "sbt-scala3-migrate" % "0.4.4")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
