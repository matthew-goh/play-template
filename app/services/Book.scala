package services

import play.api.libs.json.{Json, OFormat}

case class Book(id: String, volumeInfo: VolumeInfo)
// id, name, description, pageCount

case class VolumeInfo(title: String, description: Option[String], pageCount: Int)

object Book {
  implicit val bookFormat: OFormat[Book] = Json.format[Book]
  implicit val volumeInfoFormat: OFormat[VolumeInfo] = Json.format[VolumeInfo]
}
