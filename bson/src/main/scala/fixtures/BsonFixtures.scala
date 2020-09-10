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

package reactivemongo.extensions.fixtures

import play.api.libs.json.JsObject
import reactivemongo.api.DB
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.compat.ExtendedJsonConverters

import scala.concurrent.{ ExecutionContext, Future }

class BsonFixtures(db: => Future[DB])(implicit ec: ExecutionContext) extends Fixtures[BSONDocument] {
  def map(document: JsObject): BSONDocument = ExtendedJsonConverters.toDocument(document)

  def bulkInsert(collectionName: String, documents: LazyList[BSONDocument]): Future[Int] =
    db.flatMap(_.collection[BSONCollection](collectionName).insert(ordered = true).many(documents).map(_.n))

  def removeAll(collectionName: String): Future[WriteResult] =
    db.flatMap(_.collection[BSONCollection](collectionName).delete.one(BSONDocument.empty))

  def drop(collectionName: String): Future[Boolean] =
    db.flatMap(_.collection[BSONCollection](collectionName).drop(failIfNotFound = true))

}

object BsonFixtures {
  def apply(db: Future[DB])(implicit ec: ExecutionContext): BsonFixtures = new BsonFixtures(db)
}
