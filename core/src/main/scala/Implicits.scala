package reactivemongo.extensions

import org.joda.time.DateTime
import play.api.libs.json.{ JsNumber, JsValue, Writes }

import scala.concurrent.{ ExecutionContext, Future }

object Implicits {

	implicit class FutureOption[T](future: Future[Option[T]])(implicit ec: ExecutionContext) {

		def unary_~ : Future[T] = future.map(_.get)
	}

}
