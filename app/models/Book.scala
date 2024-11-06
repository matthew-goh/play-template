package models

import play.api.libs.json.{Json, OFormat}

// id, name, description, pageCount
case class Book(_id: String, name: String, description: Option[String], pageCount: Int)
object Book {
  implicit val formats: OFormat[Book] = Json.format[Book]
}

//case class Book(id: String, volumeInfo: VolumeInfo)
//
//case class VolumeInfo(title: String, description: Option[String], pageCount: Int)
//
//object Book {
//  implicit val bookFormat: OFormat[Book] = Json.format[Book]
//  implicit val volumeInfoFormat: OFormat[VolumeInfo] = Json.format[VolumeInfo]
//}
