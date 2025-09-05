addSbtPlugin("com.github.sbt"        % "sbt-native-packager" % "1.11.3")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"        % "2.5.5")
addSbtPlugin("org.scalameta"         % "sbt-mdoc"            % "2.7.2")
addSbtPlugin("com.typesafe.sbt"      % "sbt-git"             % "1.0.2")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"         % "1.20.0")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"        % "0.14.3")
addSbtPlugin("io.gatling"            % "gatling-sbt"         % "4.17.2")
addSbtPlugin("org.typelevel"         % "sbt-fs2-grpc"        % "2.8.4")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "2.11.1")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
