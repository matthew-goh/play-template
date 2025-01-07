package services

import baseSpec.BaseSpec
import cats.data.EitherT
import connectors.LibraryConnector
import models.{APIError, Book, Collection, DataModel, VolumeInfo}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class LibraryServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {
  // mocking: explicitly tell the methods in LibraryConnector what to return, so that you can test how LibraryService responds independently
  val mockConnector = mock[LibraryConnector]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new LibraryService(mockConnector)

//  val gameOfThrones: JsValue = Json.obj(
//    "_id" -> "someId",
//    "name" -> "A Game of Thrones",
//    "description" -> "The best book!!!",
//    "pageCount" -> 100
//  )
  // {"_id":"someId","name":"A Game of Thrones","description":"The best book!!!","pageCount":100}

  "getGoogleCollection" should {
    val url: String = "testUrl"

    "return a Collection" in {
      (mockConnector.get[Collection](_: String)(_: OFormat[Collection], _: ExecutionContext))
        .expects(url, *, *) // can take *, which shows that the connector can expect any request in place of the parameter. You might sometimes see this as any().
        .returning(EitherT.rightT(Json.parse(LibraryServiceSpec.testAPIResultStr).as[Collection])) // explicitly states what the connector method returns
        .once() // how many times we can expect this response

      // allows for the result to be waited for as the Future type can be seen as a placeholder for a value we don't have yet
      whenReady(testService.getGoogleCollection(urlOverride = Some(url), search = "", term = "").value) { result =>
        result shouldBe Right(LibraryServiceSpec.testAPICollection)
      }
    }

    "return an error" in {
      (mockConnector.get[Book](_: String)(_: OFormat[Book], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.leftT(APIError.BadAPIResponse(500, "Could not connect")))// How do we return an error?
        .once()

      whenReady(testService.getGoogleCollection(urlOverride = Some(url), search = "", term = "").value) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect"))
      }
    }
  }

  "getGoogleCollection (version called by ApplicationController searchGoogleAndDisplay())" should {
    "return a Collection" in {
      val reqBody = Some(Map(
        "search" -> List("something"),
        "keyword" -> List("inauthor"),
        "term_value" -> List("something")
      ))
      val url = "https://www.googleapis.com/books/v1/volumes?q=something%inauthor:something"

      (mockConnector.get[Collection](_: String)(_: OFormat[Collection], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT(Json.parse(LibraryServiceSpec.testAPIResultStr).as[Collection]))
        .once()

      whenReady(testService.getGoogleCollection(reqBody = reqBody).value) { result =>
        result shouldBe Right(LibraryServiceSpec.testAPICollection)
      }
    }

    "return an error from the connector" in {
      val reqBody = Some(Map(
        "search" -> List("something"),
        "keyword" -> List("inauthor"),
        "term_value" -> List("something")
      ))
      val url = "https://www.googleapis.com/books/v1/volumes?q=something%inauthor:something"

      (mockConnector.get[Collection](_: String)(_: OFormat[Collection], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.leftT(APIError.BadAPIResponse(500, "Could not connect")))
        .once()

      whenReady(testService.getGoogleCollection(reqBody = reqBody).value) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect"))
      }
    }

    "return an error if keyword is blank" in {
      val reqBody = Some(Map(
        "search" -> List("something"),
        "keyword" -> List(""),
        "term_value" -> List("something")
      ))

      whenReady(testService.getGoogleCollection(reqBody = reqBody).value) { result =>
        result shouldBe Left(APIError.BadAPIResponse(400, "Keyword missing from search"))
      }
    }
  }

  "convertBookToDataModel" should {
    "return a DataModel with the correct field values" in {
      testService.convertBookToDataModel(LibraryServiceSpec.testAPIBook) shouldBe LibraryServiceSpec.testAPIDataModel
    }
    "return a DataModel with empty description if description is missing from VolumeInfo" in {
      testService.convertBookToDataModel(LibraryServiceSpec.testAPIBookNoDesc) shouldBe LibraryServiceSpec.testAPIDataModelNoDesc
    }
  }

  "extractBooksFromCollection" should {
    "convert a Collection into a list of DataModel objects" in {
      testService.extractBooksFromCollection(LibraryServiceSpec.testAPICollection) shouldBe(Seq(LibraryServiceSpec.testAPIDataModel))
    }
  }
}

object LibraryServiceSpec {
  val testAPIResultStr: String = """{
    "kind": "books#volumes",
    "totalItems": 1,
    "items": [
      {
        "kind": "books#volume",
        "id": "1GIrEAAAQBAJ",
        "etag": "YRo3QBZZBzI",
        "selfLink": "https://www.googleapis.com/books/v1/volumes/1GIrEAAAQBAJ",
        "volumeInfo": {
          "title": "The Decagon House Murders",
          "authors": [
            "Yukito Ayatsuji"
          ],
          "publisher": "Pushkin Vertigo",
          "publishedDate": "2021-05-25",
          "description": "\"Ayatsuji's brilliant and richly atmospheric puzzle will appeal to fans of golden age whodunits...\"",
          "industryIdentifiers": [
            {
              "type": "ISBN_13",
              "identifier": "9781782276340"
            },
            {
              "type": "ISBN_10",
              "identifier": "1782276343"
            }
          ],
          "readingModes": {
            "text": false,
            "image": false
          },
          "pageCount": 289,
          "printType": "BOOK",
          "categories": [
            "Fiction"
          ],
          "maturityRating": "NOT_MATURE",
          "allowAnonLogging": false,
          "contentVersion": "0.3.0.0.preview.0",
          "panelizationSummary": {
            "containsEpubBubbles": false,
            "containsImageBubbles": false
          },
          "imageLinks": {
            "smallThumbnail": "http://books.google.com/books/content?id=1GIrEAAAQBAJ&printsec=frontcover&img=1&zoom=5&source=gbs_api",
            "thumbnail": "http://books.google.com/books/content?id=1GIrEAAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
          },
          "language": "en",
          "previewLink": "http://books.google.co.uk/books?id=1GIrEAAAQBAJ&dq=isbn:9781782276340&hl=&cd=1&source=gbs_api",
          "infoLink": "http://books.google.co.uk/books?id=1GIrEAAAQBAJ&dq=isbn:9781782276340&hl=&source=gbs_api",
          "canonicalVolumeLink": "https://books.google.com/books/about/The_Decagon_House_Murders.html?hl=&id=1GIrEAAAQBAJ"
        },
        "saleInfo": {
          "country": "GB",
          "saleability": "NOT_FOR_SALE",
          "isEbook": false
        },
        "accessInfo": {
          "country": "GB",
          "viewability": "NO_PAGES",
          "embeddable": false,
          "publicDomain": false,
          "textToSpeechPermission": "ALLOWED",
          "epub": {
            "isAvailable": false
          },
          "pdf": {
            "isAvailable": true
          },
          "webReaderLink": "http://play.google.com/books/reader?id=1GIrEAAAQBAJ&hl=&source=gbs_api",
          "accessViewStatus": "NONE",
          "quoteSharingAllowed": false
        },
        "searchInfo": {
          "textSnippet": "As the students are picked off one by one, he weaves in the story of the mainland investigation of the earlier murders. This is a homage to Golden Age detective fiction, but it’s also unabashed entertainment.&quot;"
        }
      }
    ]
  }"""

  val testAPIVolumeInfo: VolumeInfo = VolumeInfo("The Decagon House Murders", Some("\"Ayatsuji's brilliant and richly atmospheric puzzle will appeal to fans of golden age whodunits...\""), 289)
  val testAPIVolumeInfoNoDesc: VolumeInfo = VolumeInfo("The Decagon House Murders", None, 289)

  val testAPIBook: Book = Book("1GIrEAAAQBAJ", testAPIVolumeInfo)
  val testAPIBookNoDesc: Book = Book("1GIrEAAAQBAJ", testAPIVolumeInfoNoDesc)

  val testAPIItems: Seq[Book] = Seq(testAPIBook)
  val testAPICollection: Collection = Collection("books#volumes", 1, Some(testAPIItems))

  val testAPIDataModel: DataModel = DataModel("1GIrEAAAQBAJ", "The Decagon House Murders",
    "\"Ayatsuji's brilliant and richly atmospheric puzzle will appeal to fans of golden age whodunits...\"", 289)
  val testAPIDataModelNoDesc: DataModel = DataModel("1GIrEAAAQBAJ", "The Decagon House Murders", "", 289)
}
