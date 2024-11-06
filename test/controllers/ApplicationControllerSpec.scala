package controllers

import baseSpec.BaseSpecWithApplication
import models.DataModel
import org.mongodb.scala.{MongoClient, MongoDatabase}
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers._
import repositories.DataRepository
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWithApplication {
  val TestApplicationController = new ApplicationController(
    repository,
    component // comes from BaseSpecWithApplication
  )
//  val BadTestApplicationController = new ApplicationController(
//    dataRepository = new DataRepository(MongoComponent(MongoDatabase = "??")),
//    component // comes from BaseSpecWithApplication
//  )

  private val dataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )
  private val newDataModel: DataModel = DataModel(
    "abcd",
    "new name",
    "new description",
    100
  )

  "ApplicationController .index()" should {
    "list all books in the database" in {
      beforeEach()
      // need to use .create before we can find something in our repository
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
      status(indexResult) shouldBe Status.OK
      // same as status(indexResult) shouldBe 501
      // println(contentAsJson(indexResult)) // this is a Seq[DataModel]
      contentAsJson(indexResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
      afterEach()
    }

    // CAN THIS BE TESTED?
//    "return an error if the database could not be found" in {
//      beforeEach()
//      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
//      println(contentAsJson(indexResult))
//      status(indexResult) shouldBe 404
//      println(status(indexResult))
//      afterEach()
//    }
  }

  "ApplicationController .create()" should {
    "create a book in the database" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))

      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED
      afterEach()
    }

    "return a BadRequest if the book could not be created" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson("abcd"))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.BAD_REQUEST
      afterEach()
    }
  }

  "ApplicationController .read()" should {
    "find a book in the database by id" in {
      beforeEach()
      // need to use .create before we can find something in our repository
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      //Hint: You could use status(createdResult) shouldBe Status.CREATED to check this has worked again

      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[DataModel] shouldBe dataModel
      // {"_id":"abcd","name":"test name","description":"test description","pageCount":100}
      afterEach()
    }

    "return a failed Future if the book could not be found" in {
      beforeEach()
      val readResult: Future[Result] = TestApplicationController.read("aaaa")(FakeRequest())
//      println(readResult)
//      println(readResult.failed.futureValue)
      readResult.failed.futureValue shouldBe a[scala.MatchError]
      afterEach()
    }
  }

  "ApplicationController .update()" should {
    "update a book in the database" in {
      beforeEach()
      // need to use .create before we can update something in our repository
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val updateRequest: FakeRequest[JsValue] = buildPost("/api/${dataModel._id}").withBody[JsValue](Json.toJson(newDataModel))
      val updateResult = TestApplicationController.update("abcd")(updateRequest)
      status(updateResult) shouldBe Status.ACCEPTED
      contentAsJson(updateResult).as[DataModel] shouldBe newDataModel
      afterEach()
    }

    "return a BadRequest if the book could not be updated" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val badUpdateRequest: FakeRequest[JsValue] = buildPost("/api/${dataModel._id}").withBody[JsValue](Json.toJson("abcd"))
      val badUpdateResult = TestApplicationController.update("abcd")(badUpdateRequest)
      status(badUpdateResult) shouldBe Status.BAD_REQUEST
      afterEach()
    }

    "creates the book in the database if it could not be found" in { // upsert(true)
      beforeEach()
      val updateRequest: FakeRequest[JsValue] = buildPost("/api/${dataModel._id}").withBody[JsValue](Json.toJson(newDataModel))
      val updateResult = TestApplicationController.update("abcd")(updateRequest) // Future(<not completed>)
      status(updateResult) shouldBe Status.ACCEPTED
      contentAsJson(updateResult).as[DataModel] shouldBe newDataModel
      afterEach()
    }
  }

  "ApplicationController .delete()" should {
    "delete a book in the database" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val deleteResult: Future[Result] = TestApplicationController.delete("abcd")(FakeRequest())
      status(deleteResult) shouldBe Status.ACCEPTED

      // check that database is now empty
      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
      status(indexResult) shouldBe Status.OK
      contentAsJson(indexResult).as[Seq[DataModel]] shouldBe Seq()
      afterEach()
    }

    "do nothing if the book could not be found" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val deleteResult: Future[Result] = TestApplicationController.delete("aaaa")(FakeRequest())
      status(deleteResult) shouldBe Status.ACCEPTED

      // check that database still contains dataModel
      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
      status(indexResult) shouldBe Status.OK
      contentAsJson(indexResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
      afterEach()
    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())
}
