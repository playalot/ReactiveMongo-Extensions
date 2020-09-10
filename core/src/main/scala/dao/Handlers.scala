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

  implicit object BSONIntegerHandler extends BSONReader[Int] {
    def readTry(bson: BSONValue) = bson.asOpt[BSONNumberLike] match {
      case Some(num) => num.toInt
      case _ =>
        bson match {
          case doc @ BSONDocument(_) =>
            doc.asInstanceOf[BSONDocument].getAsTry[BSONNumberLike]("$int").map(_.toInt).get
        }
    }
  }

  implicit object BSONLongHandler extends BSONReader[Long] {
    def readTry(bson: BSONValue) = bson.asOpt[BSONNumberLike] match {
      case Some(num) => num.toLong
      case _ =>
        bson match {
          case doc @ BSONDocument(_) =>
            doc.asInstanceOf[BSONDocument].getAsTry[BSONNumberLike]("$long").map(_.toLong).get
        }
    }
  }

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
