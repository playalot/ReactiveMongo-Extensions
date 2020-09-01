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
  val reactiveMongoVersion = "1.0.0"
  val playReactiveMongoVersion = "0.20.12-play28-fix1"
}
