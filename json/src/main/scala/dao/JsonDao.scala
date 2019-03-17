// Copyright (C) 2014 Fehmi Can Saglam (@fehmicans) and contributors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package reactivemongo.extensions.json.dao

import scala.util.Random
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import reactivemongo.api.{ Cursor, DB, DefaultDB, QueryOpts }
import reactivemongo.api.indexes.Index
import reactivemongo.api.commands.{ WriteConcern, WriteResult }
import reactivemongo.play.json._
import reactivemongo.extensions.dao.{ Dao, LifeCycle, ReflexiveLifeCycle }
import reactivemongo.extensions.json.dsl.JsonDsl._
import play.api.libs.json.{ JsObject, Json, OFormat, OWrites, Writes }
import play.api.libs.iteratee.Iteratee
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.iteratees.cursorProducer

/** A DAO implementation that operates on JSONCollection using JsObject.
 *
 *  To create a DAO for a concrete model extend this class.
 *
 *  Below is a sample model.
 *  {{{
 *  import reactivemongo.bson.BSONObjectID
 *  import play.api.libs.json.Json
 *  import play.modules.reactivemongo.json.BSONFormats._
 *
 *  case class Person(
 *  _id: BSONObjectID = BSONObjectID.generate,
 *  name: String,
 *  surname: String,
 *  age: Int)
 *
 *  object Person {
 *  implicit val personFormat = Json.format[Person]
 *  }
 *
 *  }}}
 *
 *  To define a JsonDao for the Person model you just need to extend JsonDao.
 *
 *  {{{
 *  import reactivemongo.api.{ MongoDriver, DB }
 *  import reactivemongo.bson.BSONObjectID
 *  import play.modules.reactivemongo.json.BSONFormats._
 *  import reactivemongo.extensions.json.dao.JsonDao
 *  import scala.concurrent.ExecutionContext.Implicits.global
 *
 *
 *  object MongoContext {
 *  val driver = new MongoDriver
 *  val connection = driver.connection(List("localhost"))
 *  def db(): DB = connection("reactivemongo-extensions")
 *  }
 *
 *  object PersonDao extends JsonDao[Person, BSONObjectID](MongoContext.db, "persons")
 *  }}}
 *
 *  @param database A parameterless function returning a [[Future[reactivemongo.api.DB]] instance.
 *  @param collectionName Name of the collection this DAO is going to operate on.
 *  @param lifeCycle [[reactivemongo.extensions.dao.LifeCycle]] for the Model type.
 *  @tparam Model Type of the model that this DAO uses.
 *  @tparam ID Type of the ID field of the model.
 */
abstract class JsonDao[Model: OFormat, ID: Writes](database: => Future[DB], collectionName: String)(implicit lifeCycle: LifeCycle[Model, ID] = new ReflexiveLifeCycle[Model, ID], ec: ExecutionContext)
	extends Dao[JSONCollection, JsObject, Model, ID, OWrites](
		database, collectionName) {

	BSONDocumentWrites

	def ensureIndexes()(implicit ec: ExecutionContext): Future[Traversable[Boolean]] = Future sequence {
		autoIndexes map { index =>
			collection.flatMap(_.indexesManager.ensure(index))
		}
	}.map { results =>
		lifeCycle.ensuredIndexes()
		results
	}

	def listIndexes()(implicit ec: ExecutionContext): Future[List[Index]] =
		collection.flatMap(_.indexesManager.list())

	def findOne(selector: JsObject = Json.obj())(implicit ec: ExecutionContext): Future[Option[Model]] =
		collection.flatMap(_.find(selector).one[Model])

	def findById(id: ID)(implicit ec: ExecutionContext): Future[Option[Model]] =
		findOne($id(id))

	def findByIds(ids: ID*)(implicit ec: ExecutionContext): Future[List[Model]] =
		findAll("_id" $in (ids: _*))

	def find(
		selector: JsObject = Json.obj(),
		sort: JsObject = Json.obj("_id" -> 1),
		page: Int,
		pageSize: Int)(implicit ec: ExecutionContext): Future[List[Model]] = {
		val from = (page - 1) * pageSize
		collection.flatMap(_
			.find(selector)
			.sort(sort)
			.options(QueryOpts(skipN = from, batchSizeN = pageSize))
			.cursor[Model]()
			.collect[List](pageSize, Cursor.FailOnError[List[Model]]()))
	}

	def findAll(
		selector: JsObject = Json.obj(),
		sort: JsObject = Json.obj("_id" -> 1))(implicit ec: ExecutionContext): Future[List[Model]] = {
		collection.flatMap(_.find(selector).sort(sort).cursor[Model]().collect[List](Int.MaxValue, FailOnError[List[Model]]()))
	}

	def findAndUpdate(
		query: JsObject,
		update: JsObject,
		sort: JsObject = Json.obj(),
		fetchNewObject: Boolean = false,
		upsert: Boolean = false)(implicit ec: ExecutionContext): Future[Option[Model]] = collection.flatMap(_.findAndUpdate(
		query, update, fetchNewObject, upsert).map(_.result[Model]))

	def findAndRemove(query: JsObject, sort: JsObject = Json.obj())(implicit ec: ExecutionContext): Future[Option[Model]] = collection.flatMap(_.findAndRemove(
		query, if (sort == Json.obj()) None else Some(sort)).
		map(_.result[Model]))

	def findRandom(selector: JsObject = Json.obj())(implicit ec: ExecutionContext): Future[Option[Model]] = for {
		count <- count(selector)
		index = Random.nextInt(count)
		random <- collection.flatMap(_.find(selector).options(QueryOpts(skipN = index, batchSizeN = 1)).one[Model])
	} yield random

	def insert(model: Model, writeConcern: WriteConcern = defaultWriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
		val mappedModel = lifeCycle.prePersist(model)
		collection.flatMap(_.insert.one(mappedModel) map { writeResult =>
			lifeCycle.postPersist(mappedModel)
			writeResult
		})
	}

	def bulkInsert(documents: Iterable[Model])(implicit ec: scala.concurrent.ExecutionContext): Future[Int] = {
		val mappedDocuments = documents.map(lifeCycle.prePersist)
		collection.flatMap(_.insert(ordered = true).many(mappedDocuments)).map { result =>
			mappedDocuments.foreach(lifeCycle.postPersist)
			result.n
		}
	}

	def update[U: OWrites](
		selector: JsObject,
		update: U,
		writeConcern: WriteConcern = defaultWriteConcern,
		upsert: Boolean = false,
		multi: Boolean = false)(implicit ec: ExecutionContext): Future[WriteResult] = collection.flatMap(_.update(selector, update, writeConcern, upsert, multi))

	def updateById[U: OWrites](
		id: ID,
		update: U,
		writeConcern: WriteConcern = defaultWriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = collection.flatMap(_.update($id(id), update, writeConcern))

	def save(id: ID, model: Model, writeConcern: WriteConcern = defaultWriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
		val mappedModel = lifeCycle.prePersist(model)
		collection.flatMap(_.update($id(id), mappedModel, writeConcern, upsert = true) map { lastError =>
			lifeCycle.postPersist(mappedModel)
			lastError
		})
	}

	def count(selector: JsObject = Json.obj())(implicit ec: ExecutionContext): Future[Int] = collection.flatMap(_.count(Some(selector)))

	def drop()(implicit ec: ExecutionContext): Future[Boolean] = collection.flatMap(_.drop(failIfNotFound = true))

	def dropSync(timeout: Duration = 10 seconds)(implicit ec: ExecutionContext): Unit = Await.result(drop(), timeout)

	def removeById(id: ID)(implicit ec: ExecutionContext): Future[WriteResult] = {
		lifeCycle.preRemove(id)
		collection.flatMap(_.delete(ordered = false).one($id(id)) map { lastError =>
			lifeCycle.postRemove(id)
			lastError
		})
	}

	def remove(
		query: JsObject,
		limit: Option[Int] = None)(implicit ec: ExecutionContext): Future[WriteResult] = {
		collection.flatMap(_.delete().one(query, limit, collation = None))
	}

	def removeAll()(implicit ec: ExecutionContext): Future[WriteResult] = {
		collection.flatMap(_.delete().one(Json.obj(), None, None))
	}

	def foreach(
		selector: JsObject = Json.obj(),
		sort: JsObject = Json.obj("_id" -> 1))(f: Model => Unit)(implicit ec: ExecutionContext): Future[Unit] = {
		collection.flatMap { c =>
			val enumerator = c.find(selector).sort(sort).cursor[Model]().enumerator()
			val process: Iteratee[Model, Unit] = Iteratee.foreach(f)
			enumerator.run(process)
		}
	}

	def fold[A](
		selector: JsObject = Json.obj(),
		sort: JsObject = Json.obj("_id" -> 1),
		state: A)(f: (A, Model) => A)(implicit ec: ExecutionContext): Future[A] = {
		collection.flatMap { c =>
			val enumerator = c.find(selector).sort(sort).cursor[Model]().enumerator()
			val process: Iteratee[Model, A] = Iteratee.fold(state)(f)
			enumerator.run(process)
		}
	}

	ensureIndexes()
}

object JsonDao {
	def apply[Model: OFormat, ID: Writes](db: => Future[DefaultDB], collectionName: String)(
		implicit
		lifeCycle: LifeCycle[Model, ID] = new ReflexiveLifeCycle[Model, ID], ec: ExecutionContext): JsonDao[Model, ID] = new JsonDao[Model, ID](db, collectionName) {}

}
