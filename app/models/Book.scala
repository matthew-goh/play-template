package models

import play.api.libs.json.{JsValue, Json, OFormat}

case class Book(id: String, volumeInfo: VolumeInfo)

object Book {
  implicit val formats: OFormat[Book] = Json.format[Book]
}
