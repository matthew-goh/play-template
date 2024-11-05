package controllers

import baseSpec.BaseSpecWithApplication
import org.mongodb.scala.{MongoClient, MongoDatabase}
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.test.Helpers._
import repositories.DataRepository
import uk.gov.hmrc.mongo.MongoComponent

class ApplicationControllerSpec extends BaseSpecWithApplication {
  val TestApplicationController = new ApplicationController(
    repository,
    component // comes from BaseSpecWithApplication
  )

  "ApplicationController .index()" should {
    val result = TestApplicationController.index()(FakeRequest()) // outcome of calling index() on the controller
    // FakeRequest() mimics an incoming HTTP request

    "return 200 OK" in {
      // shouldBe is a way of doing assertions
      status(result) shouldBe Status.OK
      // same as status(result) shouldBe 501
    }
  }

  "ApplicationController .create()" should {

  }

  "ApplicationController .read()" should {

  }

  "ApplicationController .update()" should {

  }

  "ApplicationController .delete()" should {

  }
}
