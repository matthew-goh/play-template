package models

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.{Json, OFormat}

case class DataModel(_id: String, name: String, description: String, pageCount: Int)

object DataModel {
  implicit val formats: OFormat[DataModel] = Json.format[DataModel]

  val dataForm: Form[DataModel] = Form(
    mapping(
      "_id" -> nonEmptyText,
      "name" -> nonEmptyText,
      "description" -> text,
      "pageCount" -> number(min = 1, max = 2000)
    )(DataModel.apply)(DataModel.unapply)
  )
}