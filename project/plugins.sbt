addSbtPlugin("org.scalameta"    % "sbt-scalafmt"       % "2.4.3")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"           % "2.2.24")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"            % "1.0.2")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"        % "1.7.1")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"       % "0.9.31")
addSbtPlugin("ch.epfl.scala"    % "sbt-scala3-migrate" % "0.4.6")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
