addSbtPlugin("com.github.sbt"        % "sbt-native-packager"   % "1.11.7")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"          % "2.5.6")
addSbtPlugin("org.scalameta"         % "sbt-mdoc"              % "2.8.2")
addSbtPlugin("com.typesafe.sbt"      % "sbt-git"               % "1.0.2")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"           % "1.20.2")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"          % "0.14.5")
addSbtPlugin("io.gatling"            % "gatling-sbt"           % "4.17.12")
addSbtPlugin("org.typelevel"         % "sbt-fs2-grpc"          % "3.0.0")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt"   % "3.0.0")
addSbtPlugin("com.github.sbt.junit"  % "sbt-jupiter-interface" % "0.17.0")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.1"
