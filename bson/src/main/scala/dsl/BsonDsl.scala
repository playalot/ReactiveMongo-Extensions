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

// In order to use Producer.produce we must be in this package.
package reactivemongo.bson

trait BsonDsl {

  implicit def bsonDocumentToPretty(document: BSONDocument): String = {
    BSONDocument.pretty(document)
  }

  protected def produce(producer: Producer[BSONValue]): BSONValue = {
    producer.produce.get
  }

  def $doc(item: Producer[BSONElement], items: Producer[BSONElement]*): BSONDocument = {
    BSONDocument((Seq(item) ++ items): _*)
  }

  def $and(item: Producer[BSONElement], items: Producer[BSONElement]*): BSONDocument = {
    $doc(item, items: _*)
  }

  def $docx(field: String, element: BSONElement, elements: BSONElement*): BSONDocument = {
    BSONDocument(field -> BSONDocument((Seq(element) ++ elements)))
  }

  def $id(id: Producer[BSONValue]): BSONDocument = {
    BSONDocument("_id" -> id.produce.get)
  }

  def $exists(field: String, exists: Boolean = true): BSONDocument = {
    $doc(field -> $doc("$exists" -> exists))
  }

  def $ne(item: Producer[BSONElement]): BSONDocument = {
    val (field, value) = item.produce.get
    BSONDocument(field -> BSONDocument("$ne" -> value))
  }

  def $gt(item: Producer[BSONElement]): BSONDocument = {
    val (field, value) = item.produce.get
    BSONDocument(field -> BSONDocument("$gt" -> value))
  }

  def $gtx(value: Producer[BSONValue]): BSONElement = {
    "$gt" -> value.produce.get
  }

  def $gte(item: Producer[BSONElement]): BSONDocument = {
    val (field, value) = item.produce.get
    BSONDocument(field -> BSONDocument("$gte" -> value))
  }

  def $gtex(value: Producer[BSONValue]): BSONElement = {
    "$gte" -> value.produce.get
  }

  def $in(field: String, values: Producer[BSONValue]): BSONDocument = {
    BSONDocument(field -> BSONDocument("$in" -> values.produce.get))
  }

  def $lt(item: Producer[BSONElement]): BSONDocument = {
    val (field, value) = item.produce.get
    BSONDocument(field -> BSONDocument("$lt" -> value))
  }

  def $ltx(value: Producer[BSONValue]): BSONElement = {
    "$lt" -> value.produce.get
  }

  def $lte(item: Producer[BSONElement]): BSONDocument = {
    val (field, value) = item.produce.get
    BSONDocument(field -> BSONDocument("$lte" -> value))
  }

  def $ltex(value: Producer[BSONValue]): BSONElement = {
    "$lte" -> value.produce.get
  }

  def $nin(field: String, values: Producer[BSONValue]): BSONDocument = {
    BSONDocument(field -> BSONDocument("$nin" -> values.produce.get))
  }

  def $set(item: Producer[BSONElement], items: Producer[BSONElement]*): BSONDocument = {
    BSONDocument("$set" -> BSONDocument((Seq(item) ++ items): _*))
  }

  def $unset(field: String, fields: String*): BSONDocument = {
    BSONDocument("$unset" -> BSONDocument((Seq(field) ++ fields).map(_ -> BSONString(""))))
  }

  def $push(item: Producer[BSONElement]): BSONDocument = {
    BSONDocument("$push" -> BSONDocument(item))
  }

  def $pushEach(field: String, values: Producer[BSONValue]*): BSONDocument = {
    BSONDocument(
      "$push" -> BSONDocument(
        field -> BSONDocument(
          "$each" -> BSONArray(values.map(_.produce.get))
        )
      )
    )
  }

  def $pull(item: Producer[BSONElement]): BSONDocument = {
    BSONDocument("$pull" -> BSONDocument(item))
  }

  def $or(expressions: BSONDocument*): BSONDocument = {
    BSONDocument("$or" -> expressions)
  }

  def $regex(field: String, value: String, options: String) = {
    BSONDocument(field -> BSONRegex(value, options))
  }

}

object BsonDsl extends BsonDsl
