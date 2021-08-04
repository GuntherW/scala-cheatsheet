addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.4.3")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"           % "2.2.22")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"            % "1.0.1")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.7.0")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"       % "0.9.29")
addSbtPlugin("ch.epfl.scala"    % "sbt-scala3-migrate" % "0.4.5")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
