scalaVersion := "2.13.7"

name := "hello-world"
organization := "ch.epfl.scala"
version := "1.0"

val AkkaVersion = "2.7.0"

libraryDependencies ++= Seq(
    // DB stuff
    "com.typesafe.slick" %% "slick" % "3.3.3",
    "org.postgresql" % "postgresql" % "42.3.4",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
    // slick pg supports additional data types like date/time
    "com.github.tminglei" %% "slick-pg" % "0.20.3",
    // http libraries
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "com.typesafe.akka" %% "akka-http" % "10.4.0",
    // json
    "com.typesafe.play" %% "play-json" % "2.8.2",
    // akka json support
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.4.0",
    // Testing
    "com.typesafe.akka" %% "akka-http-testkit" % "10.4.0" % Test,
    "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.14" % Test,
    "org.scalamock" %% "scalamock" % "5.1.0" % Test
    // logback
    // Uncomment when needed.
    // "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime
)
