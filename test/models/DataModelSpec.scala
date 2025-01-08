package models

import baseSpec.BaseSpec
import eu.timepit.refined.auto._
import play.api.data._
import play.api.data.Forms._

import scala.collection.immutable.ArraySeq

class DataModelSpec extends BaseSpec {
  val addBookForm: Form[DataModel] = DataModel.dataForm
  lazy val formData: DataModel = DataModel("abcd", "Test Title", "test description", 100)
//  lazy val formDataInvalid: DataModel = DataModel("", "", "", -1) // does not compile with refined types

  "Add book form" should {
    "bind" when {
      "with a valid response" in {
        val completedForm = addBookForm.bind(Map("_id" -> "abcd",
          "name" -> "Test Title",
          "description" -> "test description",
          "pageCount" -> "100"))

        completedForm.value shouldBe Some(formData)
        completedForm.errors shouldBe List.empty
        completedForm.data shouldBe Map("_id" -> "abcd",
          "name" -> "Test Title",
          "description" -> "test description",
          "pageCount" -> "100")
      }

      "with a invalid response (missing required fields and negative page count)" in {
        val completedForm = addBookForm.bind(Map("_id" -> "",
          "name" -> "",
          "description" -> "",
          "pageCount" -> "-1"))

        completedForm.value shouldBe None
        completedForm.errors shouldBe List(FormError("_id", List("error.required"), List()),
          FormError("name", List("error.required"), List()),
          FormError("pageCount", List("error.min"), List("0")))
      }

      "with a invalid response (non-integer page count)" in {
        val completedForm = addBookForm.bind(Map("_id" -> "abcd",
          "name" -> "Test Title",
          "description" -> "",
          "pageCount" -> "1.5"))

        completedForm.value shouldBe None
        completedForm.errors shouldBe List(FormError("pageCount", List("error.number"), List()))
      }

      "with no response" in {
        val completedForm = addBookForm.bind(Map.empty[String, String])
        completedForm.value shouldBe None
        completedForm.errors shouldBe List(FormError("_id", List("error.required"), List()),
          FormError("name", List("error.required"), List()),
          FormError("description", List("error.required"), List()),
          FormError("pageCount", List("error.required"), List()))
      }
    }

    "fill" when {
      "with a valid response" in {
        val filledForm = addBookForm.fill(formData)
        filledForm.value shouldBe Some(formData)
        filledForm.errors shouldBe List.empty
        filledForm.data shouldBe Map("_id" -> "abcd",
          "name" -> "Test Title",
          "description" -> "test description",
          "pageCount" -> "100")
      }

//      "with an invalid response (unsubmitted)" in {
//        val filledForm = addBookForm.fill(formDataInvalid)
//        filledForm.value shouldBe Some(formDataInvalid)
//        filledForm.errors shouldBe List.empty
//        filledForm.data shouldBe Map("_id" -> "",
//          "name" -> "",
//          "description" -> "",
//          "pageCount" -> "-1")
//      }
    }
  }
}
