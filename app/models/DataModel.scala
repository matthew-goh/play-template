package models

import be.venneborg.refined.play.RefinedJsonFormats._
import be.venneborg.refined.play.RefinedForms._
import models.RefinedTypes.{NonEmptyString, PageCount}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.{Json, OFormat}

case class DataModel(_id: NonEmptyString, name: NonEmptyString, description: String, pageCount: PageCount)

object DataModel {
  implicit val formats: OFormat[DataModel] = Json.format[DataModel]

  val dataForm: Form[DataModel] = Form(
    mapping(
      "_id" -> Forms.of[NonEmptyString],
      "name" -> Forms.of[NonEmptyString],
      "description" -> text,
      "pageCount" -> Forms.of[PageCount]
    )(DataModel.apply)(DataModel.unapply)
  )
}
