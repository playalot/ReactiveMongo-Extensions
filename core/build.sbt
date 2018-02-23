name := "reactivemongo-extensions-core"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % Common.reactiveMongoVersion,
  "org.reactivemongo" %% "reactivemongo-iteratees" % "0.12.7",
  "com.typesafe.play" %% "play-json" % Common.playVersion % "provided",
  "com.typesafe.play" %% "play-iteratees" % "2.6.1" % "provided",
  "com.typesafe" % "config" % "1.3.2",
  "joda-time" % "joda-time" % "2.9.9",
  "org.joda" % "joda-convert" % "1.9.2",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test")
