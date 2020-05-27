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

  val playJsonVersion = "2.9.0"
  val reactiveMongoVersion = "0.19.3"
  val playReactiveMongoVersion = "0.19.3-play27"
}
