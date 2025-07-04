addSbtPlugin("com.github.sbt"        % "sbt-native-packager" % "1.11.1")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"        % "2.5.5")
addSbtPlugin("org.scalameta"         % "sbt-mdoc"            % "2.7.1")
addSbtPlugin("com.typesafe.sbt"      % "sbt-git"             % "1.0.2")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"         % "1.19.0")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"        % "0.14.3")
addSbtPlugin("io.gatling"            % "gatling-sbt"         % "4.16.1")
addSbtPlugin("org.typelevel"         % "sbt-fs2-grpc"        % "2.8.2")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt" % "2.10.1")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
