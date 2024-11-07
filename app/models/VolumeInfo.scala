package models

import play.api.libs.json.{Json, OFormat}

case class VolumeInfo(title: String, description: Option[String], pageCount: Int)

object VolumeInfo {
  implicit val formats: OFormat[VolumeInfo] = Json.format[VolumeInfo]
}
