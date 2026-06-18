addSbtPlugin("com.github.sbt"        % "sbt-native-packager"   % "1.11.7")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"          % "2.6.1")
addSbtPlugin("org.scalameta"         % "sbt-mdoc"              % "2.9.0")
addSbtPlugin("com.github.sbt"        % "sbt-git"               % "2.1.0")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"           % "1.21.0")
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"          % "0.14.7")
addSbtPlugin("io.gatling"            % "gatling-sbt"           % "4.18.3")
addSbtPlugin("org.typelevel"         % "sbt-fs2-grpc"          % "3.1.2")
addSbtPlugin("com.github.ghostdogpr" % "caliban-codegen-sbt"   % "3.1.2")
addSbtPlugin("com.github.sbt.junit"  % "sbt-jupiter-interface" % "0.19.0")
addSbtPlugin("pl.project13.scala"    % "sbt-jmh"               % "0.4.8")

libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.1"
