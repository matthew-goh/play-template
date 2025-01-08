package models

import baseSpec.BaseSpec
import eu.timepit.refined.auto._
import play.api.libs.json.{JsValue, Json}
import services.LibraryServiceSpec

class CollectionSpec extends BaseSpec {
  "casting an API result to a Collection" should {
    "cast the value of 'items' into a list of books, which in turn each contain a VolumeInfo" in {
      Json.parse(LibraryServiceSpec.testAPIResultStr).as[Collection] shouldBe
        Collection("books#volumes", 1, Some(Seq(Book("1GIrEAAAQBAJ", LibraryServiceSpec.testAPIVolumeInfo))))
    }

    "make items None if totalItems is 0" in {
      val testAPIResultNoItems: JsValue = Json.parse("""{"kind": "books#volumes", "totalItems": 0}""")
      testAPIResultNoItems.as[Collection] shouldBe Collection("books#volumes", 0, None)
    }
  }
}
