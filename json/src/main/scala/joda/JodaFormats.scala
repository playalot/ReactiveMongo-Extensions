package reactivemongo.extensions.json.joda

import org.joda.time.DateTime
import play.api.libs.json.Format

object JodaFormats {
	implicit val datetimeFormat: Format[DateTime] = {
		Format(JodaReads.DefaultJodaDateTimeReads, JodaWrites.JodaDateTimeNumberWrites)
	}
}
