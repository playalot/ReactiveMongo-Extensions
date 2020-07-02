name := "reactivemongo-extensions-bson"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo-bson-api" % Common.reactiveMongoVersion,
  "org.reactivemongo" %% "reactivemongo-bson-macros" % Common.reactiveMongoVersion
)
