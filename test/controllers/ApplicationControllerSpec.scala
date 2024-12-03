package controllers

import baseSpec.BaseSpecWithApplication
import cats.data.EitherT
import models.{APIError, Collection, DataModel}
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.LibraryServiceSpec
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc._
import play.api.test.Helpers._
import services.LibraryService

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

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
  private val dataModel2: DataModel = DataModel(
    "123",
    "The Decagon House Murders",
    "test description 2",
    289
  )

  "ApplicationController .index()" should {
    "list all books in the database" in {
      // need to use .create before we can find something in our repository
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
      status(indexResult) shouldBe Status.OK
      // same as status(indexResult) shouldBe 501
      contentAsJson(indexResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
    }
  }

  "ApplicationController .create()" should {
    "create a book in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))

//      val createdResultAwait: Result = Await.result(TestApplicationController.create()(request), 100.milliseconds)
//      createdResultAwait.header.status shouldBe Status.CREATED
//      // Extract the response body from the Result
//      val responseBody: String = createdResultAwait.body match {
//        case HttpEntity.Strict(data, _) => data.decodeString("UTF-8")
//        case _                          => throw new IllegalStateException("Unexpected response entity type")
//      }
//      // Parse the response body as JSON and cast to DataModel
//      Json.parse(responseBody).as[DataModel] shouldBe dataModel

      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED
      contentAsJson(createdResult).as[DataModel] shouldBe dataModel
    }

    "return an InternalServerError if the book ID is already in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val duplicateRequest: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val duplicateResult: Future[Result] = TestApplicationController.create()(duplicateRequest)
      status(duplicateResult) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(duplicateResult) shouldBe "Bad response from upstream; got status: 500, and got reason: Book already exists in database"
    }

    "return a BadRequest if the request body could not be parsed into a DataModel" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson("abcd"))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.BAD_REQUEST
      contentAsString(createdResult) shouldBe "Invalid request body"
    }
  }

  "ApplicationController .read()" should {
    "find a book in the database by id" in {
      // need to use .create before we can find something in our repository
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[DataModel] shouldBe dataModel
      // {"_id":"abcd","name":"test name","description":"test description","pageCount":100}
    }

    "return a NotFound if the book could not be found" in {
      val readResult: Future[Result] = TestApplicationController.read("aaaa")(FakeRequest())
      status(readResult) shouldBe NOT_FOUND
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 404, and got reason: Book not found"
    }
  }

  "ApplicationController .readBySpecificField()" should {
    "find a book in the database by id" in {
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.readBySpecifiedField("_id", "abcd")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
    }

    "find a book in the database by name" in {
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      Thread.sleep(100)
      val readResult: Future[Result] = TestApplicationController.readBySpecifiedField("name", "Test Name")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
    }

    "find a book in the database by description" in {
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      Thread.sleep(100)
      val readResult: Future[Result] = TestApplicationController.readBySpecifiedField("description", "test DESCRIPTION")(FakeRequest())
      status(readResult) shouldBe Status.OK
      contentAsJson(readResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
    }

    "return a BadRequest if an invalid field is specified" in {
      val readResult: Future[Result] = TestApplicationController.readBySpecifiedField("bad", "qqq")(FakeRequest())
      status(readResult) shouldBe Status.BAD_REQUEST
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 400, and got reason: Invalid field to search"
    }
  }

  "ApplicationController .update()" should {
    "update a book in the database" in {
      // need to use .create before we can update something in our repository
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val updateRequest: FakeRequest[JsValue] = buildPost("/api/${dataModel._id}").withBody[JsValue](Json.toJson(newDataModel))
      val updateResult = TestApplicationController.update("abcd")(updateRequest)
      status(updateResult) shouldBe Status.ACCEPTED
      contentAsJson(updateResult).as[DataModel] shouldBe newDataModel
    }

    "return a BadRequest if the if the request body could not be parsed into a DataModel" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val badUpdateRequest: FakeRequest[JsValue] = buildPost("/api/${dataModel._id}").withBody[JsValue](Json.toJson("abcd"))
      val badUpdateResult = TestApplicationController.update("abcd")(badUpdateRequest)
      status(badUpdateResult) shouldBe Status.BAD_REQUEST
      contentAsString(badUpdateResult) shouldBe "Invalid request body"
    }

//    "add the book to the database if it could not be found" in { // upsert(true)
//      val updateRequest: FakeRequest[JsValue] = buildPost("/api/${dataModel._id}").withBody[JsValue](Json.toJson(newDataModel))
//      val updateResult = TestApplicationController.update("abcd")(updateRequest) // Future(<not completed>)
//      status(updateResult) shouldBe Status.ACCEPTED
//      contentAsJson(updateResult).as[DataModel] shouldBe newDataModel
//    }
    "return a NotFound if the book could not be found" in { // upsert(false)
      val updateRequest: FakeRequest[JsValue] = buildPost("/api/${dataModel._id}").withBody[JsValue](Json.toJson(newDataModel))
      val updateResult = TestApplicationController.update("abcd")(updateRequest)
      status(updateResult) shouldBe Status.NOT_FOUND
      contentAsString(updateResult) shouldBe "Bad response from upstream; got status: 404, and got reason: Book not found"

      // check that database is still empty
      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
      status(indexResult) shouldBe Status.OK
      contentAsJson(indexResult).as[Seq[DataModel]] shouldBe Seq()
    }
  }

  "ApplicationController .updateWithValue()" should {
    "update a book's name in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val updateResult = TestApplicationController.updateWithValue("abcd", "name", "New Name")(FakeRequest())
      status(updateResult) shouldBe Status.ACCEPTED
      contentAsString(updateResult) shouldBe "name of book abcd has been updated to: New Name"
    }

    "update a book's page count in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val updateResult = TestApplicationController.updateWithValue("abcd", "pageCount", "200")(FakeRequest())
      status(updateResult) shouldBe Status.ACCEPTED
      contentAsString(updateResult) shouldBe "pageCount of book abcd has been updated to: 200"
    }

    "return a BadRequest if an invalid field is specified" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.updateWithValue("abcd", "bad", "qqq")(FakeRequest())
      status(readResult) shouldBe Status.BAD_REQUEST
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 400, and got reason: Invalid field to update"
    }

    "return a BadRequest if page count is updated with a non-integer value" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.updateWithValue("abcd", "pageCount", "1xx")(FakeRequest())
      status(readResult) shouldBe Status.BAD_REQUEST
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 400, and got reason: Page count must be an integer"
    }

    "return a NotFound if the book does not exist in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val readResult: Future[Result] = TestApplicationController.updateWithValue("aaaa", "pageCount", "100")(FakeRequest())
      status(readResult) shouldBe Status.NOT_FOUND
      contentAsString(readResult) shouldBe "Bad response from upstream; got status: 404, and got reason: Book not found"
    }
  }

  "ApplicationController .delete()" should {
    "delete a book in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val deleteResult: Future[Result] = TestApplicationController.delete("abcd")(FakeRequest())
      status(deleteResult) shouldBe Status.ACCEPTED
      contentAsString(deleteResult) shouldBe "Book abcd has been deleted"

      // check that database is now empty
      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
      status(indexResult) shouldBe Status.OK
      contentAsJson(indexResult).as[Seq[DataModel]] shouldBe Seq()
    }

//    "do nothing if the book could not be found" in {
//      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
//      val createdResult: Future[Result] = TestApplicationController.create()(request)
//
//      val deleteResult: Future[Result] = TestApplicationController.delete("aaaa")(FakeRequest())
//      status(deleteResult) shouldBe Status.ACCEPTED
//
//      // check that database still contains dataModel
//      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
//      status(indexResult) shouldBe Status.OK
//      contentAsJson(indexResult).as[Seq[DataModel]] shouldBe Seq(dataModel)
//    }

    "return a NotFound if the book could not be found" in {
      val deleteResult: Future[Result] = TestApplicationController.delete("aaaa")(FakeRequest())
      status(deleteResult) shouldBe Status.NOT_FOUND
      contentAsString(deleteResult) shouldBe "Bad response from upstream; got status: 404, and got reason: Book not found"
    }
  }

  "ApplicationController .getGoogleCollection()" should {
    "return a Collection" in {
      (mockLibraryService.getGoogleCollection(_: Option[String], _: String, _: String)(_: ExecutionContext))
        .expects(None, *, *, *)
        .returning(EitherT.rightT(LibraryServiceSpec.testAPIResult.as[Collection]))
        .once()

      val collectionResult: Future[Result] = TestApplicationController.getGoogleCollection(search = "", term = "")(FakeRequest())
      status(collectionResult) shouldBe OK
      contentAsJson(collectionResult) shouldBe Json.toJson(LibraryServiceSpec.testAPICollection)
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
      status(extractionResult) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(extractionResult) shouldBe "Bad response from upstream; got status: 500, and got reason: Could not connect"
    }
  }

  ///// METHODS CALLED BY FRONTEND /////
  "ApplicationController .listAllBooks()" should {
    "list all books in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      val request2: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel2))
      val createdResult2: Future[Result] = TestApplicationController.create()(request2)

      val listingResult: Future[Result] = TestApplicationController.listAllBooks()(FakeRequest())
      status(listingResult) shouldBe Status.OK
      contentAsString(listingResult) should include ("test description")
      contentAsString(listingResult) should include ("The Decagon House Murders")
    }

    "show 'No books found' if the database is empty" in {
      val listingResult: Future[Result] = TestApplicationController.listAllBooks()(FakeRequest())
      status(listingResult) shouldBe Status.OK
      contentAsString(listingResult) should include ("No books found")
    }
  }

  "ApplicationController .showBookDetails()" should {
    "display the specified book's details" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val searchResult: Future[Result] = TestApplicationController.showBookDetails("abcd")(FakeRequest())
      status(searchResult) shouldBe Status.OK
      contentAsString(searchResult) should include ("test name")
    }

    "return a NotFound if the book is not in the database" in {
      val searchResult: Future[Result] = TestApplicationController.showBookDetails("abcd")(FakeRequest())
      status(searchResult) shouldBe Status.NOT_FOUND
      contentAsString(searchResult) should include ("Not found")
    }
  }

  "ApplicationController .searchBookByID()" should {
    "redirect to book details page when an ID is searched" in {
      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchid").withFormUrlEncodedBody(
        "bookID" -> "abcd"
      )
      val searchResult: Future[Result] = TestApplicationController.searchBookByID()(searchRequest)
      status(searchResult) shouldBe Status.SEE_OTHER
      redirectLocation(searchResult) shouldBe Some("/bookdetails/abcd")
    }

    "return a BadRequest if ID is blank" in {
      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchid").withFormUrlEncodedBody(
        "bookID" -> ""
      )
      val searchResult: Future[Result] = TestApplicationController.searchBookByID()(searchRequest)
      status(searchResult) shouldBe Status.BAD_REQUEST
      contentAsString(searchResult) should include ("No ID provided")
    }
  }

  "ApplicationController .searchBookByTitle()" should {
    "list the matching books" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      val request2: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(newDataModel))
      val createdResult2: Future[Result] = TestApplicationController.create()(request2)

      Thread.sleep(100)
      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchtitle").withFormUrlEncodedBody(
        "title" -> "test"
      ) // .withCRSFToken not needed?
      val searchResult: Future[Result] = TestApplicationController.searchBookByTitle()(searchRequest)
      status(searchResult) shouldBe Status.OK
      contentAsString(searchResult) should include ("test name")
      contentAsString(searchResult) shouldNot include ("new name")
    }

    "show 'No books found' if the database is empty" in {
      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchtitle").withFormUrlEncodedBody(
        "title" -> " name"
      ) // .withCRSFToken not needed?
      val searchResult: Future[Result] = TestApplicationController.searchBookByTitle()(searchRequest)
      status(searchResult) shouldBe Status.OK
      contentAsString(searchResult) should include ("No books found")
    }

    "return a BadRequest if title is blank" in {
      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchid").withFormUrlEncodedBody(
        "title" -> ""
      )
      val searchResult: Future[Result] = TestApplicationController.searchBookByTitle()(searchRequest)
      status(searchResult) shouldBe Status.BAD_REQUEST
      contentAsString(searchResult) should include ("No title provided")
    }
  }

  "ApplicationController .searchGoogleAndDisplay()" should {
    "list the API search results without adding books to the database" in {
      val reqBody = Some(Map(
        "search" -> List("something"),
        "keyword" -> List("inauthor"),
        "term_value" -> List("something")
      ))

      (mockLibraryService.getGoogleCollection(_: Option[Map[String, Seq[String]]])(_: ExecutionContext))
        .expects(reqBody, *)
        .returning(EitherT.rightT(LibraryServiceSpec.testAPICollection))
        .once()

      (mockLibraryService.extractBooksFromCollection(_: Collection))
        .expects(LibraryServiceSpec.testAPICollection)
        .returning(Seq(dataModel, dataModel2))
        .once()

      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchgoogle").withFormUrlEncodedBody(
        "search" -> "something",
        "keyword" -> "inauthor",
        "term_value" -> "something"
      ) // .withCRSFToken not needed?
      val searchResult: Future[Result] = TestApplicationController.searchGoogleAndDisplay()(searchRequest)
      status(searchResult) shouldBe Status.OK
      contentAsString(searchResult) should include ("test description")
      contentAsString(searchResult) should include ("The Decagon House Murders")
      contentAsString(searchResult) should include ("Add to database")

      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
      status(indexResult) shouldBe Status.OK
      contentAsJson(indexResult).as[Seq[DataModel]] shouldBe Seq() // database should be empty
    }

    "list the API search results and add the books to the database" in {
      val reqBody = Some(Map(
        "search" -> List("something"),
        "keyword" -> List("inauthor"),
        "term_value" -> List("something"),
        "add_to_database" -> List("true")
      ))

      (mockLibraryService.getGoogleCollection(_: Option[Map[String, Seq[String]]])(_: ExecutionContext))
        .expects(reqBody, *)
        .returning(EitherT.rightT(LibraryServiceSpec.testAPICollection))
        .once()

      (mockLibraryService.extractBooksFromCollection(_: Collection))
        .expects(LibraryServiceSpec.testAPICollection)
        .returning(Seq(dataModel, dataModel2))
        .once()

      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchgoogle").withFormUrlEncodedBody(
        "search" -> "something",
        "keyword" -> "inauthor",
        "term_value" -> "something",
        "add_to_database" -> "true"
      ) // .withCRSFToken not needed?
      val searchResult: Future[Result] = TestApplicationController.searchGoogleAndDisplay()(searchRequest)
      status(searchResult) shouldBe Status.OK
      contentAsString(searchResult) should include ("test description")
      contentAsString(searchResult) should include ("The Decagon House Murders")
      contentAsString(searchResult) shouldNot include ("Add to database")

      Thread.sleep(100) // allow time to add books to database
      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())
      status(indexResult) shouldBe Status.OK
      contentAsJson(indexResult).as[Seq[DataModel]] should contain theSameElementsAs Seq(dataModel, dataModel2) // database should contain the 2 books
    }

    "show 'No books found' if there are no search results" in {
      val reqBody = Some(Map(
        "search" -> List("something"),
        "keyword" -> List("inauthor"),
        "term_value" -> List("something")
      ))

      (mockLibraryService.getGoogleCollection(_: Option[Map[String, Seq[String]]])(_: ExecutionContext))
        .expects(reqBody, *)
        .returning(EitherT.rightT(LibraryServiceSpec.testAPICollection))
        .once()

      (mockLibraryService.extractBooksFromCollection(_: Collection))
        .expects(*)
        .returning(Seq())
        .once()

      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchgoogle").withFormUrlEncodedBody(
        "search" -> "something",
        "keyword" -> "inauthor",
        "term_value" -> "something"
      ) // .withCRSFToken not needed?
      val searchResult: Future[Result] = TestApplicationController.searchGoogleAndDisplay()(searchRequest)
      status(searchResult) shouldBe Status.OK
      contentAsString(searchResult) should include ("No books found")
    }

    "return a BadRequest if keyword is blank" in {
      val reqBody = Some(Map(
        "search" -> List("something"),
        "keyword" -> List(""),
        "term_value" -> List("something")
      ))

      (mockLibraryService.getGoogleCollection(_: Option[Map[String, Seq[String]]])(_: ExecutionContext))
        .expects(reqBody, *)
        .returning(EitherT.leftT(APIError.BadAPIResponse(400, "Keyword missing from search")))
        .once()

      val searchRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/searchgoogle").withFormUrlEncodedBody(
        "search" -> "something",
        "keyword" -> "",
        "term_value" -> "something"
      ) // .withCRSFToken not needed?
      val searchResult: Future[Result] = TestApplicationController.searchGoogleAndDisplay()(searchRequest)
      status(searchResult) shouldBe Status.BAD_REQUEST
      contentAsString(searchResult) should include ("Keyword missing from search")
    }
  }

  "ApplicationController .addFromSearch()" should {
    "add a book to the database" in {
      val addBookRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/addfromsearch").withFormUrlEncodedBody(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "pageCount" -> "100"
      ) // .withCRSFToken not needed?
      val addBookResult: Future[Result] = TestApplicationController.addFromSearch()(addBookRequest)
      status(addBookResult) shouldBe Status.OK
      contentAsString(addBookResult) should include ("test name")
    }

    "return an InternalServerError if the book ID is already in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val addBookRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/addfromsearch").withFormUrlEncodedBody(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "pageCount" -> "100"
      ) // .withCRSFToken not needed?
      val addBookResult: Future[Result] = TestApplicationController.addFromSearch()(addBookRequest)
      status(addBookResult) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(addBookResult) should include ("Book already exists in database")
    }
  }

  "ApplicationController .addBookForm()" should {
    "add a book to the database" in {
      val addBookRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/add/form").withFormUrlEncodedBody(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "pageCount" -> "100"
      ) // .withCRSFToken not needed?
      val addBookResult: Future[Result] = TestApplicationController.addBookForm()(addBookRequest)
      status(addBookResult) shouldBe Status.OK
      // println(contentAsString(addBookResult))
    }

    "detect a form with errors" in {
      val addBookRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/add/form").withFormUrlEncodedBody(
        "_id" -> "abcd",
        "name" -> "",
        "description" -> "test description",
        "pageCount" -> "100"
      ) // .withCRSFToken not needed?
      val addBookResult: Future[Result] = TestApplicationController.addBookForm()(addBookRequest)
      status(addBookResult) shouldBe Status.BAD_REQUEST
    }

    "return an InternalServerError if the book ID is already in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val addBookRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/add/form").withFormUrlEncodedBody(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "pageCount" -> "100"
      ) // .withCRSFToken not needed?
      val addBookResult: Future[Result] = TestApplicationController.addBookForm()(addBookRequest)
      status(addBookResult) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(addBookResult) should include ("Book already exists in database")
    }
  }

  "ApplicationController .updateBookForm()" should {
    "update a book in the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val updateBookRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/update/form").withFormUrlEncodedBody(
        "_id" -> "abcd",
        "name" -> "new name",
        "description" -> "new description",
        "pageCount" -> "200"
      ) // .withCRSFToken not needed?
      val updateBookResult: Future[Result] = TestApplicationController.updateBookForm()(updateBookRequest)
      status(updateBookResult) shouldBe Status.OK
      contentAsString(updateBookResult) should include ("new name")
    }

    "detect a form with errors" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val updateBookRequest: FakeRequest[AnyContentAsFormUrlEncoded] = buildPost("/add/form").withFormUrlEncodedBody(
        "_id" -> "abcd",
        "name" -> "new name",
        "description" -> "new description",
        "pageCount" -> "aaa"
      ) // .withCRSFToken not needed?
      val updateBookResult: Future[Result] = TestApplicationController.updateBookForm()(updateBookRequest)
      status(updateBookResult) shouldBe Status.BAD_REQUEST
    }
  }

  "ApplicationController .deleteBook()" should {
    "delete a book from the database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      val deleteResult: Future[Result] = TestApplicationController.deleteBook("abcd")(FakeRequest())
      status(deleteResult) shouldBe Status.OK
      contentAsString(deleteResult) should include ("Delete successful!")
    }

    "return a NotFound if the book could not be found" in {
      val deleteResult: Future[Result] = TestApplicationController.deleteBook("aaaa")(FakeRequest())
      status(deleteResult) shouldBe Status.NOT_FOUND
      contentAsString(deleteResult) should include ("Book not found")
    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())
}
