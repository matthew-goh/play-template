package models

import play.api.libs.json.{JsValue, Json, OFormat}

case class Collection(kind: String, totalItems: Int, items: Option[JsValue])

object Collection {
  implicit val formats: OFormat[Collection] = Json.format[Collection]
}
