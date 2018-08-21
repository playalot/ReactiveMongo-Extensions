name := "reactivemongo-extensions-core"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % Common.reactiveMongoVersion,
  "org.reactivemongo" %% "reactivemongo-iteratees" % Common.reactiveMongoVersion,
  "com.typesafe.play" %% "play-json" % Common.playJsonVersion % "provided",
  "com.typesafe.play" %% "play-iteratees" % "2.6.1" % "provided",
  "com.typesafe" % "config" % "1.3.3",
  "joda-time" % "joda-time" % "2.10",
  "org.joda" % "joda-convert" % "2.1",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test")
