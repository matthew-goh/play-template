package models

import be.venneborg.refined.play.RefinedJsonFormats._
import models.RefinedTypes.NonEmptyString
import play.api.libs.json.{JsValue, Json, OFormat}

case class Book(id: NonEmptyString, volumeInfo: VolumeInfo)

object Book {
  implicit val formats: OFormat[Book] = Json.format[Book]
}
