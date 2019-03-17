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

package reactivemongo.extensions.dao

import scala.util.Random
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import reactivemongo.bson._
import reactivemongo.api.{ Cursor, DefaultDB, QueryOpts }
import reactivemongo.api.indexes.Index
import reactivemongo.api.commands.{ WriteConcern, WriteResult }
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.extensions.dsl.BsonDsl._
import play.api.libs.iteratee.Iteratee
import reactivemongo.play.iteratees.cursorProducer
import Handlers._
import reactivemongo.api.Cursor.FailOnError

/** A DAO implementation operates on BSONCollection using BSONDocument.
 *
 *  To create a DAO for a concrete model extend this class.
 *
 *  Below is a sample model.
 *  {{{
 *  import reactivemongo.bson._
 *  import reactivemongo.extensions.dao.Handlers._
 *
 *  case class Person(
 *  _id: BSONObjectID = BSONObjectID.generate,
 *  name: String,
 *  surname: String,
 *  age: Int)
 *
 *  object Person {
 *  implicit val personHandler = Macros.handler[Person]
 *  }
 *  }}}
 *
 *  To define a BsonDao for the Person model you just need to extend BsonDao.
 *
 *  {{{
 *  import reactivemongo.api.{ MongoDriver, DB }
 *  import reactivemongo.bson.BSONObjectID
 *  import reactivemongo.bson.DefaultBSONHandlers._
 *  import reactivemongo.extensions.dao.BsonDao
 *  import scala.concurrent.ExecutionContext.Implicits.global
 *
 *  object MongoContext {
 *  val driver = new MongoDriver
 *  val connection = driver.connection(List("localhost"))
 *  def db(): DB = connection("reactivemongo-extensions")
 *  }
 *
 *  object PersonDao extends BsonDao[Person, BSONObjectID](MongoContext.db, "persons")
 *  }}}
 *
 *  @param db A parameterless function returning a [[reactivemongo.api.DB]] instance.
 *  @param collectionName Name of the collection this DAO is going to operate on.
 *  @param modelReader BSONDocumentReader for the Model type.
 *  @param modelWriter BSONDocumentWriter for the Model type.
 *  @param idWriter BSONDocumentWriter for the ID type.
 *  @param lifeCycle [[reactivemongo.extensions.dao.LifeCycle]] for the Model type.
 *  @tparam Model Type of the model that this DAO uses.
 *  @tparam ID Type of the ID field of the model.
 */
abstract class BsonDao[Model, ID](db: => Future[DefaultDB], collectionName: String)(implicit
		modelReader: BSONDocumentReader[Model],
		modelWriter: BSONDocumentWriter[Model],
		idWriter: BSONWriter[ID, _ <: BSONValue],
		idReader: BSONReader[_ <: BSONValue, ID],
		lifeCycle: LifeCycle[Model, ID] = new ReflexiveLifeCycle[Model, ID],
		ec: ExecutionContext)
	extends Dao[BSONCollection, BSONDocument, Model, ID, BSONDocumentWriter](db, collectionName) {

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

	def findOne(selector: BSONDocument = BSONDocument.empty)(implicit ec: ExecutionContext): Future[Option[Model]] = collection.flatMap(_.find(selector).one[Model])

	def findById(id: ID)(implicit ec: ExecutionContext): Future[Option[Model]] =
		findOne($id(id))

	def findByIds(ids: ID*)(implicit ec: ExecutionContext): Future[List[Model]] =
		findAll("_id" $in (ids: _*))

	def find(
		selector: BSONDocument = BSONDocument.empty,
		sort: BSONDocument = BSONDocument("_id" -> 1),
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
		selector: BSONDocument = BSONDocument.empty,
		sort: BSONDocument = BSONDocument("_id" -> 1))(implicit ec: ExecutionContext): Future[List[Model]] =
		collection.flatMap(_.find(selector).sort(sort).cursor[Model]().collect[List](Int.MaxValue, FailOnError[List[Model]]()))

	def findAndUpdate(
		query: BSONDocument,
		update: BSONDocument,
		sort: BSONDocument = BSONDocument.empty,
		fetchNewObject: Boolean = false,
		upsert: Boolean = false)(implicit ec: ExecutionContext): Future[Option[Model]] = collection.flatMap(_.findAndUpdate(
		query, update, fetchNewObject, upsert).map(_.result[Model]))

	def findAndRemove(query: BSONDocument, sort: BSONDocument = BSONDocument.empty)(implicit ec: ExecutionContext): Future[Option[Model]] =
		collection.flatMap(_.findAndRemove(
			query, if (sort == BSONDocument.empty) None else Some(sort)).
			map(_.result[Model]))

	def findRandom(selector: BSONDocument = BSONDocument.empty)(implicit ec: ExecutionContext): Future[Option[Model]] = for {
		count <- count(selector)
		index = if (count == 0) 0 else Random.nextInt(count)
		random <- collection.flatMap(_.find(selector).options(QueryOpts(skipN = index, batchSizeN = 1)).one[Model])
	} yield random

	def insert(model: Model, writeConcern: WriteConcern = defaultWriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
		val mappedModel = lifeCycle.prePersist(model)
		collection.flatMap(_.insert.one(mappedModel) map { writeResult =>
			lifeCycle.postPersist(mappedModel)
			writeResult
		})
	}

	def bulkInsert(documents: Iterable[Model])(implicit ec: ExecutionContext): Future[Int] = {
		val mappedDocuments = documents.map(lifeCycle.prePersist)
		collection.flatMap(_.insert(ordered = true).many(mappedDocuments)).map { result =>
			mappedDocuments.foreach(lifeCycle.postPersist)
			result.n
		}
	}

	def update[U: BSONDocumentWriter](
		selector: BSONDocument,
		update: U,
		writeConcern: WriteConcern = defaultWriteConcern,
		upsert: Boolean = false,
		multi: Boolean = false)(implicit ec: ExecutionContext): Future[WriteResult] = collection.flatMap(_.update(selector, update, writeConcern, upsert, multi))

	def updateById[U: BSONDocumentWriter](
		id: ID,
		update: U,
		writeConcern: WriteConcern = defaultWriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = collection.flatMap(_.update($id(id), update, writeConcern))

	def save(id: ID, model: Model, writeConcern: WriteConcern = defaultWriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
		val writer = implicitly[BSONDocumentWriter[Model]]
		for {
			doc <- Future(writer write model)
			_id <- Future(doc.getAs[ID]("_id").get)
			res <- {
				val mappedModel = lifeCycle.prePersist(model)
				collection.flatMap(_.update(selector = $id(_id), update = mappedModel,
					upsert = true, writeConcern = writeConcern) map { result =>
					lifeCycle.postPersist(mappedModel)
					result
				})
			}
		} yield res
	}

	def count(selector: BSONDocument = BSONDocument.empty)(implicit ec: ExecutionContext): Future[Int] = collection.flatMap(_.count(Some(selector)))

	def drop()(implicit ec: ExecutionContext): Future[Boolean] = collection.flatMap(_.drop(failIfNotFound = true))

	def dropSync(timeout: Duration = 10 seconds)(implicit ec: ExecutionContext): Unit = Await.result(drop(), timeout)

	def removeById(id: ID)(implicit ec: ExecutionContext): Future[WriteResult] = {
		lifeCycle.preRemove(id)
		collection.flatMap(_.delete(ordered = false).one($id(id)) map { res =>
			lifeCycle.postRemove(id)
			res
		})
	}

	def remove(
		query: BSONDocument,
		limit: Option[Int] = None)(implicit ec: ExecutionContext): Future[WriteResult] = collection.flatMap(_.delete().one(query, limit, collation = None))

	def removeAll()(implicit ec: ExecutionContext): Future[WriteResult] = {
		collection.flatMap(_.delete().one(BSONDocument.empty, None, None))
	}

	def foreach(
		selector: BSONDocument = BSONDocument.empty,
		sort: BSONDocument = BSONDocument("_id" -> 1))(f: Model => Unit)(implicit ec: ExecutionContext): Future[Unit] = {
		collection.flatMap { c =>
			val enumerator = c.find(selector).sort(sort).cursor[Model]().enumerator()
			val process: Iteratee[Model, Unit] = Iteratee.foreach(f)
			enumerator.run(process)
		}
	}

	def fold[A](
		selector: BSONDocument = BSONDocument.empty,
		sort: BSONDocument = BSONDocument("_id" -> 1),
		state: A)(f: (A, Model) => A)(implicit ec: ExecutionContext): Future[A] = {
		collection.flatMap { c =>
			val enumerator = c.find(selector).sort(sort).cursor[Model]().enumerator()
			val process: Iteratee[Model, A] = Iteratee.fold(state)(f)
			enumerator.run(process)
		}
	}

	ensureIndexes()
}

object BsonDao {
	def apply[Model, ID](db: => Future[DefaultDB], collectionName: String)(
		implicit
		modelReader: BSONDocumentReader[Model],
		modelWriter: BSONDocumentWriter[Model],
		idWriter: BSONWriter[ID, _ <: BSONValue],
		idReader: BSONReader[_ <: BSONValue, ID],
		lifeCycle: LifeCycle[Model, ID] = new ReflexiveLifeCycle[Model, ID],
		ec: ExecutionContext): BsonDao[Model, ID] = {
		new BsonDao[Model, ID](db, collectionName) {}
	}
}
