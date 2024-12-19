addSbtPlugin("com.github.sbt"        % "sbt-native-packager" % "1.10.4")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"        % "2.5.2")
addSbtPlugin("org.scalameta"         % "sbt-mdoc"            % "2.6.2")
addSbtPlugin("com.typesafe.sbt"      % "sbt-git"             % "1.0.2")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"         % "1.17.0")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"        % "0.13.0")
addSbtPlugin("io.gatling"            % "gatling-sbt"         % "4.11.1")
addSbtPlugin("org.typelevel"         % "sbt-fs2-grpc"        % "2.7.21")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "2.9.0")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
