name := "reactivemongo-extensions-core"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % Common.reactiveMongoVersion,
//  "org.reactivemongo" %% "reactivemongo-iteratees" % Common.reactiveMongoVersion,
  "com.typesafe.play" %% "play-json" % Common.playJsonVersion % "provided",
  "com.typesafe" % "config" % "1.4.0",
  "joda-time" % "joda-time" % "2.10.6",
  "org.slf4j" % "slf4j-api" % "1.7.29",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
  "org.scalatest" %% "scalatest" % "3.1.0" % "test")
