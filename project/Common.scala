import sbt._
import Keys._

object Common {
  lazy val prompt = { state: State =>
    val extracted = Project.extract(state)
    import extracted._

    (name in currentRef get structure.data).map { name =>
      "[" + Colors.blue(name) + "] $ "
    }.getOrElse("> ")
  }

  val playJsonVersion = "2.8.0"
  val reactiveMongoVersion = "0.18.8"
  val playReactiveMongoVersion = "0.18.8-play27"
}
