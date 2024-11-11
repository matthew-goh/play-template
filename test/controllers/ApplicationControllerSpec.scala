package controllers

import baseSpec.BaseSpecWithApplication
import cats.data.EitherT
import models.{APIError, Collection, DataModel}
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.LibraryServiceSpec
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers._
import repositories.DataRepository
import services.LibraryService
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends BaseSpecWithApplication with MockFactory {
  val mockLibraryService: LibraryService = mock[LibraryService]

  val TestApplicationController = new ApplicationController(
    repoService,
    mockLibraryService,
    component // comes from BaseSpecWithApplication
  )

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
    200
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
      contentAsJson(createdResult).as[DataModel] shouldBe dataModel
      afterEach()
    }

    "return a BadRequest if the book ID is already in the database" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val duplicateRequest: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val duplicateResult: Future[Result] = TestApplicationController.create()(duplicateRequest)
      status(duplicateResult) shouldBe Status.BAD_REQUEST
      afterEach()
    }

    "return a BadRequest if the request body could not be parsed into a DataModel" in {
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

      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[DataModel] shouldBe dataModel
      // {"_id":"abcd","name":"test name","description":"test description","pageCount":100}
      afterEach()
    }

    "return a BadRequest if the book could not be found" in {
      beforeEach()
      val readResult: Future[Result] = TestApplicationController.read("aaaa")(FakeRequest())
      Thread.sleep(200)
      status(readResult) shouldBe NOT_FOUND
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 404, and got reason: Book not found"
      afterEach()
    }
  }

  "ApplicationController .readBySpecificField()" should {
    "find a book in the database by id" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.readBySpecifiedField("_id", "abcd")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
      afterEach()
    }

    "find a book in the database by name" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.readBySpecifiedField("name", "test name")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
      afterEach()
    }

    "find a book in the database by description" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.readBySpecifiedField("description", "test description")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
      afterEach()
    }

    "return a BadRequest if an invalid field is specified" in {
      beforeEach()
      val readResult: Future[Result] = TestApplicationController.readBySpecifiedField("bad", "qqq")(FakeRequest())
      status(readResult) shouldBe Status.BAD_REQUEST
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 500, and got reason: Invalid field to search"
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

    "return a BadRequest if the if the request body could not be parsed into a DataModel" in {
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

  "ApplicationController .updateWithValue()" should {
    "update a book's name in the database" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val updateResult = TestApplicationController.updateWithValue("abcd", "name", "New Name")(FakeRequest())
      status(updateResult) shouldBe Status.ACCEPTED
      contentAsString(updateResult) shouldBe "name of book abcd has been updated to: New Name"
      afterEach()
    }

    "update a book's page count in the database" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val updateResult = TestApplicationController.updateWithValue("abcd", "pageCount", "200")(FakeRequest())
      status(updateResult) shouldBe Status.ACCEPTED
      contentAsString(updateResult) shouldBe "pageCount of book abcd has been updated to: 200"
      afterEach()
    }

    "return a BadRequest if an invalid field is specified" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.updateWithValue("abcd", "bad", "qqq")(FakeRequest())
      status(readResult) shouldBe Status.BAD_REQUEST
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 500, and got reason: Invalid field to update"
      afterEach()
    }

    "return a BadRequest if page count is updated with a non-integer value" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.updateWithValue("abcd", "pageCount", "1xx")(FakeRequest())
      status(readResult) shouldBe Status.BAD_REQUEST
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 500, and got reason: Page count must be an integer"
      afterEach()
    }

    "return a BadRequest if the book does not exist in the database" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.updateWithValue("aaaa", "pageCount", "100")(FakeRequest())
      status(readResult) shouldBe Status.BAD_REQUEST
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 404, and got reason: Book not found"
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

  "ApplicationController .getGoogleCollection()" should {
    "return a Collection" in {
      (mockLibraryService.getGoogleCollection(_: Option[String], _: String, _: String)(_: ExecutionContext))
        .expects(None, *, *, *)
        .returning(EitherT.rightT(LibraryServiceSpec.testAPIResult.as[Collection]))
        .once()

      val collectionResult: Future[Result] = TestApplicationController.getGoogleCollection(search = "", term = "")(FakeRequest())
      println(collectionResult)
      status(collectionResult) shouldBe OK
      contentAsJson(collectionResult) shouldBe LibraryServiceSpec.testAPIResult
    }
  }

  "ApplicationController .getGoogleBookList()" should {
    "return a list of DataModels" in {
      (mockLibraryService.getGoogleCollection(_: Option[String], _: String, _: String)(_: ExecutionContext))
        .expects(None, *, *, *)
        .returning(EitherT.rightT(LibraryServiceSpec.testAPIResult.as[Collection]))
        .once()

      (mockLibraryService.extractBooksFromCollection(_: Collection))
        .expects(*)
        .returning(Seq(LibraryServiceSpec.testAPIDataModel))
        .once()

      val extractionResult: Future[Result] = TestApplicationController.getGoogleBookList(search = "", term = "")(FakeRequest())
      status(extractionResult) shouldBe OK
      contentAsJson(extractionResult).as[Seq[DataModel]] shouldBe Seq(LibraryServiceSpec.testAPIDataModel)
    }

    "return an error" in {
      (mockLibraryService.getGoogleCollection(_: Option[String], _: String, _: String)(_: ExecutionContext))
        .expects(None, *, *, *)
        .returning(EitherT.leftT(APIError.BadAPIResponse(500, "Could not connect")))
        .once()

      val extractionResult: Future[Result] = TestApplicationController.getGoogleBookList(search = "", term = "")(FakeRequest())
      status(extractionResult) shouldBe BAD_REQUEST
      contentAsString(extractionResult) shouldBe "Bad response from upstream; got status: 500, and got reason: Could not connect"
    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())
}
