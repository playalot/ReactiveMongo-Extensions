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

import org.joda.time.DateTime
import reactivemongo.api.bson._

import scala.util.Success

object Handlers {

  implicit object BSONDateTimeHandler extends BSONReader[DateTime] with BSONWriter[DateTime] {

    def readTry(bson: BSONValue) = bson.asTry[BSONDateTime].flatMap(_.toLong).map(new DateTime(_))

    def writeTry(date: DateTime) = Success(BSONDateTime(date.getMillis))
  }

  implicit def MapBSONReader[T](implicit reader: BSONReader[T]): BSONDocumentReader[Map[String, T]] =
    (doc: BSONDocument) => {
      Success(doc.elements.collect {
        case ele => ele.value.asOpt[T](reader).map(ov => (ele.name, ov))
      }.flatten.toMap)
    }

  implicit def MapBSONWriter[T](implicit writer: BSONWriter[T]): BSONDocumentWriter[Map[String, T]] =
    (doc: Map[String, T]) => {
      Success(BSONDocument(doc.map(t => (t._1, writer.writeOpt(t._2).get))))
    }

  implicit def MapReader[V](implicit vr: BSONDocumentReader[V]): BSONDocumentReader[Map[String, V]] =
    (bson: BSONDocument) => {
      val elements = bson.elements.map { ele =>
        // assume that all values in the document are BSONDocuments
        ele.name -> vr.readTry(ele.value.asOpt[BSONDocument].get).get
      }
      Success(elements.toMap)
    }

  implicit def MapWriter[V](implicit vw: BSONDocumentWriter[V]): BSONDocumentWriter[Map[String, V]] =
    (map: Map[String, V]) => {
      val elements = map.to(LazyList).map { tuple => tuple._1 -> vw.writeTry(tuple._2).get }
      Success(BSONDocument(elements))
    }

		def read(bson: BSONValue) = bson.asOpt[BSONNumberLike] match {
  implicit object BSONDoubleHandler extends BSONReader[Double] {
    def readTry(bson: BSONValue) = bson.asOpt[BSONNumberLike] match {
      case Some(num) => num.toDouble
      case _ =>
        bson match {
          case doc @ BSONDocument(_) =>
            doc.asInstanceOf[BSONDocument].getAsTry[BSONNumberLike]("$double").map(_.toDouble).get
        }
    }
  }

}
