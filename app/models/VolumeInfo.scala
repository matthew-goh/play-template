package models

import be.venneborg.refined.play.RefinedJsonFormats._
import models.RefinedTypes.{NonEmptyString, PageCount}
import play.api.libs.json.{Json, OFormat}

case class VolumeInfo(title: NonEmptyString, description: Option[String], pageCount: PageCount)

object VolumeInfo {
  implicit val formats: OFormat[VolumeInfo] = Json.format[VolumeInfo]
}
