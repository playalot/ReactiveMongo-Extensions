import Common.{ playJsonVersion, playReactiveMongoVersion }

name := "reactivemongo-extensions-json"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % playReactiveMongoVersion,
  "com.typesafe.play" %% "play-json" % playJsonVersion % "provided",
  "com.typesafe.play" %% "play-json-joda" % playJsonVersion % "provided"
)
