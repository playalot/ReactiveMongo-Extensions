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

import org.scalatest._
import org.scalatest.concurrent._
import org.scalatest.time.SpanSugar._
import reactivemongo.bson._
import reactivemongo.bson.Macros.Options.Verbose
import reactivemongo.extensions.model.CustomIdModel
import reactivemongo.extensions.dsl.BsonDsl._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CustomIdBsonDaoSpec
	extends AnyFlatSpec
	with Matchers
	with ScalaFutures
	with BeforeAndAfter
	with OneInstancePerTest {

	override implicit def patienceConfig = PatienceConfig(timeout = 20 seconds, interval = 1 seconds)

	val dao = new CustomIdBsonDao

	after {
		dao.dropSync()
	}

	"A CustomIdBsonDao" should "find document by id" in {
		val customIdModel = CustomIdModel(name = "foo", surname = "bar", age = 32)

		val futureResult = for {
			insertResult <- dao.insert(customIdModel)
			maybeCustomIdModel <- dao.findById(customIdModel._id)
		} yield maybeCustomIdModel

		whenReady(futureResult) { maybeCustomIdModel =>
			maybeCustomIdModel should be('defined)
			maybeCustomIdModel.get._id shouldBe customIdModel._id
			maybeCustomIdModel.get.age shouldBe customIdModel.age
		}
	}

	it should "find documents by ids" in {
		val customIdModels = CustomIdModel.random(100)

		val futureResult = for {
			insertResult <- dao.bulkInsert(customIdModels)
			models <- dao.findByIds(customIdModels.drop(5).map(_._id): _*)
		} yield models

		whenReady(futureResult) { models =>
			models should have size 95
		}
	}

	it should "update document by id" in {
		val customIdModel = CustomIdModel(name = "foo", surname = "bar", age = 32)
		val update = $set("age" -> 64)

		val futureResult = for {
			insert <- dao.insert(customIdModel)
			update <- dao.updateById(customIdModel._id, update)
			updatedMaybeCustomIdModel <- dao.findById(customIdModel._id)
		} yield updatedMaybeCustomIdModel

		whenReady(futureResult) { updatedMaybeCustomIdModel =>
			updatedMaybeCustomIdModel should be('defined)
			val updatedCustomIdModel = updatedMaybeCustomIdModel.get
			updatedCustomIdModel._id shouldBe customIdModel._id
			updatedCustomIdModel.age shouldBe 64
		}
	}

	it should "update the whole document by id" in {
		val customIdModel = CustomIdModel(name = "foo", surname = "bar", age = 32)
		val update = customIdModel.copy(age = 64)

		val futureResult = for {
			insert <- dao.insert(customIdModel)
			update <- dao.updateById(customIdModel._id, update)
			updatedMaybeCustomIdModel <- dao.findById(customIdModel._id)
		} yield updatedMaybeCustomIdModel

		whenReady(futureResult) { updatedMaybeCustomIdModel =>
			updatedMaybeCustomIdModel should be('defined)
			val updatedCustomIdModel = updatedMaybeCustomIdModel.get
			updatedCustomIdModel._id shouldBe customIdModel._id
			updatedCustomIdModel.age shouldBe 64
		}
	}

	it should "ensure indexes" in {
		val customIdModel = CustomIdModel(name = "foo", surname = "bar", age = 32)
		val futureIndexes = for {
			_ <- dao.save(customIdModel._id, customIdModel)
			_ <- dao.ensureIndexes()
			indexes <- dao.listIndexes()
		} yield indexes

		whenReady(futureIndexes) { indexes =>
			indexes should have size 3 // including _id
		}
	}

}
