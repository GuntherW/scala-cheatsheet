addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.10.0")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.5.2")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"            % "2.5.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"             % "1.0.2")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"         % "1.16.0")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"        % "0.12.0")
addSbtPlugin("io.gatling"       % "gatling-sbt"         % "4.9.0")
addSbtPlugin("org.typelevel"    % "sbt-fs2-grpc"        % "2.7.14")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
