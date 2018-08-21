import Common.{ playJsonVersion, playReactiveMongoVersion }

name := "reactivemongo-extensions-bson"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % playReactiveMongoVersion,
  "com.typesafe.play" %% "play-json" % playJsonVersion % "provided"
)
