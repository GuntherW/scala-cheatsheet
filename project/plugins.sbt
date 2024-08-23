addSbtPlugin("com.github.sbt"        % "sbt-native-packager" % "1.10.4")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"        % "2.5.2")
addSbtPlugin("org.scalameta"         % "sbt-mdoc"            % "2.5.4")
addSbtPlugin("com.typesafe.sbt"      % "sbt-git"             % "1.0.2")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"         % "1.16.0")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"        % "0.12.1")
addSbtPlugin("io.gatling"            % "gatling-sbt"         % "4.9.2")
addSbtPlugin("org.typelevel"         % "sbt-fs2-grpc"        % "2.7.17")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "2.8.1")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
