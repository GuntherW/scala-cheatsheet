addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.9.16")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.5.2")
addSbtPlugin("org.scalameta"    % "sbt-mdoc"            % "2.5.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"             % "1.0.2")
addSbtPlugin("org.scala-js"     % "sbt-scalajs"         % "1.14.0")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"        % "0.11.1")
addSbtPlugin("io.gatling"       % "gatling-sbt"         % "4.7.0")
addSbtPlugin("org.typelevel"    % "sbt-fs2-grpc"        % "2.7.11")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
