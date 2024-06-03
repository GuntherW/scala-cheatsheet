addSbtPlugin("com.github.sbt"        % "sbt-native-packager" % "1.10.0")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"        % "2.5.2")
addSbtPlugin("org.scalameta"         % "sbt-mdoc"            % "2.5.2")
addSbtPlugin("com.typesafe.sbt"      % "sbt-git"             % "1.0.2")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"         % "1.16.0")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"        % "0.12.1")
addSbtPlugin("io.gatling"            % "gatling-sbt"         % "4.9.0")
addSbtPlugin("org.typelevel"         % "sbt-fs2-grpc"        % "2.7.15")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "2.7.1")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
