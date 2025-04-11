addSbtPlugin("com.github.sbt"        % "sbt-native-packager" % "1.11.1")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"        % "2.5.4")
addSbtPlugin("org.scalameta"         % "sbt-mdoc"            % "2.7.0")
addSbtPlugin("com.typesafe.sbt"      % "sbt-git"             % "1.0.2")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"         % "1.18.2")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"        % "0.14.2")
addSbtPlugin("io.gatling"            % "gatling-sbt"         % "4.13.3")
addSbtPlugin("org.typelevel"         % "sbt-fs2-grpc"        % "2.8.0")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "2.10.0")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
