package connectors

import baseSpec.BaseSpecWithApplication
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import models.{APIError, Book, Collection}
import org.scalatest.BeforeAndAfterAll
import play.api.libs.json.Json
import services.LibraryServiceSpec

class LibraryConnectorSpec extends BaseSpecWithApplication with BeforeAndAfterAll {
  val TestLibraryConnector = new LibraryConnector(ws)

  val Port = 8080
  val Host = "localhost"
  val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(Port))

  override def beforeAll: Unit = {
    wireMockServer.start()
    configureFor(Host, Port)
  }

  override def afterAll: Unit = {
    wireMockServer.stop()
    ws.close()
  }

  "LibraryConnector .get()" should {
    "return a Right(Collection) containing a book" in {
      stubFor(get(urlEqualTo("/getgooglecollection/decagon/isbn:9781782276340"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """{
              |    "kind": "books#volumes",
              |    "totalItems": 1,
              |    "items": [
              |      {
              |        "kind": "books#volume",
              |        "id": "1GIrEAAAQBAJ",
              |        "etag": "YRo3QBZZBzI",
              |        "selfLink": "https://www.googleapis.com/books/v1/volumes/1GIrEAAAQBAJ",
              |        "volumeInfo": {
              |          "title": "The Decagon House Murders",
              |          "authors": [
              |            "Yukito Ayatsuji"
              |          ],
              |          "publisher": "Pushkin Vertigo",
              |          "publishedDate": "2021-05-25",
              |          "description": "\"Ayatsuji's brilliant and richly atmospheric puzzle will appeal to fans of golden age whodunits...\"",
              |          "industryIdentifiers": [
              |            {
              |              "type": "ISBN_13",
              |              "identifier": "9781782276340"
              |            },
              |            {
              |              "type": "ISBN_10",
              |              "identifier": "1782276343"
              |            }
              |          ],
              |          "readingModes": {
              |            "text": false,
              |            "image": false
              |          },
              |          "pageCount": 289,
              |          "printType": "BOOK",
              |          "categories": [
              |            "Fiction"
              |          ],
              |          "maturityRating": "NOT_MATURE",
              |          "allowAnonLogging": false,
              |          "contentVersion": "0.3.0.0.preview.0",
              |          "panelizationSummary": {
              |            "containsEpubBubbles": false,
              |            "containsImageBubbles": false
              |          },
              |          "imageLinks": {
              |            "smallThumbnail": "http://books.google.com/books/content?id=1GIrEAAAQBAJ&printsec=frontcover&img=1&zoom=5&source=gbs_api",
              |            "thumbnail": "http://books.google.com/books/content?id=1GIrEAAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
              |          },
              |          "language": "en",
              |          "previewLink": "http://books.google.co.uk/books?id=1GIrEAAAQBAJ&dq=isbn:9781782276340&hl=&cd=1&source=gbs_api",
              |          "infoLink": "http://books.google.co.uk/books?id=1GIrEAAAQBAJ&dq=isbn:9781782276340&hl=&source=gbs_api",
              |          "canonicalVolumeLink": "https://books.google.com/books/about/The_Decagon_House_Murders.html?hl=&id=1GIrEAAAQBAJ"
              |        },
              |        "saleInfo": {
              |          "country": "GB",
              |          "saleability": "NOT_FOR_SALE",
              |          "isEbook": false
              |        },
              |        "accessInfo": {
              |          "country": "GB",
              |          "viewability": "NO_PAGES",
              |          "embeddable": false,
              |          "publicDomain": false,
              |          "textToSpeechPermission": "ALLOWED",
              |          "epub": {
              |            "isAvailable": false
              |          },
              |          "pdf": {
              |            "isAvailable": true
              |          },
              |          "webReaderLink": "http://play.google.com/books/reader?id=1GIrEAAAQBAJ&hl=&source=gbs_api",
              |          "accessViewStatus": "NONE",
              |          "quoteSharingAllowed": false
              |        },
              |        "searchInfo": {
              |          "textSnippet": "As the students are picked off one by one, he weaves in the story of the mainland investigation of the earlier murders. This is a homage to Golden Age detective fiction, but it’s also unabashed entertainment.&quot;"
              |        }
              |      }
              |    ]
              |  }
          """.stripMargin)))

      whenReady(TestLibraryConnector.get[Collection]("http://localhost:8080/getgooglecollection/decagon/isbn:9781782276340").value) { result =>
        result shouldBe Right(Collection("books#volumes", 1, Some(Seq(Book("1GIrEAAAQBAJ", LibraryServiceSpec.testAPIVolumeInfo)))))
      }
    }

    "return a Right(Collection) without books" in {
      stubFor(get(urlEqualTo("/getgooglecollection/decagon/isbn:123"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody("""{"kind": "books#volumes", "totalItems": 0}""")))

      whenReady(TestLibraryConnector.get[Collection]("http://localhost:8080/getgooglecollection/decagon/isbn:123").value) { result =>
        result shouldBe Right(Collection("books#volumes", 0, None))
      }
    }

    "return a Could not connect error if the response cannot be mapped to a Collection" in {
      stubFor(get(urlEqualTo("/getgooglecollection/decagon/isbn:123"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """{"kind": "books#volumes",
              |  "login": "matthew-goh",
              |  "id": 186605436,
              |  "followers": 0,
              |  "following": 0
              |}""".stripMargin)))

      whenReady(TestLibraryConnector.get[Collection]("http://localhost:8080/getgooglecollection/decagon/isbn:123").value) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect"))
      }
    }
  }
}