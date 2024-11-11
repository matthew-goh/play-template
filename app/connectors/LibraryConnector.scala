package connectors

import cats.data.EitherT
import models.APIError
import play.api.libs.json.JsPath.json
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class LibraryConnector @Inject()(ws: WSClient) {
  // used for a GET method
  // implicit rds: OFormat[Response] is needed to parse the Json response model as our model
  // [Response] is a type parameter for our method, this must be defined when calling the method
  def get[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)
    val response = request.get()
    // EitherT allows us to return either Future[APIError] or Future[Response]
    EitherT {
      response
        .map {
          result => Right(result.json.as[Response])

//            // shouldn't give an error if search returned no results
//            if (result.json("totalItems").as[Int] == 0) {
//              print("Hi")
//              Right(Collection(result.json("kind").as[String], result.json("totalItems").as[Int]))
//            } else Right(result.json.as[Response])
        }
        .recover { //case _: WSResponse =>
          case _ => Left(APIError.BadAPIResponse(500, "Could not connect"))
        }
    }
  }
}

// {
//  "kind": "books#volumes",
//  "totalItems": 1,
//  "items": [
//    {
//      "kind": "books#volume",
//      "id": "1GIrEAAAQBAJ",
//      "etag": "YRo3QBZZBzI",
//      "selfLink": "https://www.googleapis.com/books/v1/volumes/1GIrEAAAQBAJ",
//      "volumeInfo": {
//        "title": "The Decagon House Murders",
//        "authors": [
//          "Yukito Ayatsuji"
//        ],
//        "publisher": "Pushkin Vertigo",
//        "publishedDate": "2021-05-25",
//        "description": "\"Ayatsuji's brilliant and richly atmospheric puzzle will appeal to fans of golden age whodunits...\"",
//        "industryIdentifiers": [
//          {
//            "type": "ISBN_13",
//            "identifier": "9781782276340"
//          },
//          {
//            "type": "ISBN_10",
//            "identifier": "1782276343"
//          }
//        ],
//        "readingModes": {
//          "text": false,
//          "image": false
//        },
//        "pageCount": 289,
//        "printType": "BOOK",
//        "categories": [
//          "Fiction"
//        ],
//        "maturityRating": "NOT_MATURE",
//        "allowAnonLogging": false,
//        "contentVersion": "0.3.0.0.preview.0",
//        "panelizationSummary": {
//          "containsEpubBubbles": false,
//          "containsImageBubbles": false
//        },
//        "imageLinks": {
//          "smallThumbnail": "http://books.google.com/books/content?id=1GIrEAAAQBAJ&printsec=frontcover&img=1&zoom=5&source=gbs_api",
//          "thumbnail": "http://books.google.com/books/content?id=1GIrEAAAQBAJ&printsec=frontcover&img=1&zoom=1&source=gbs_api"
//        },
//        "language": "en",
//        "previewLink": "http://books.google.co.uk/books?id=1GIrEAAAQBAJ&dq=isbn:9781782276340&hl=&cd=1&source=gbs_api",
//        "infoLink": "http://books.google.co.uk/books?id=1GIrEAAAQBAJ&dq=isbn:9781782276340&hl=&source=gbs_api",
//        "canonicalVolumeLink": "https://books.google.com/books/about/The_Decagon_House_Murders.html?hl=&id=1GIrEAAAQBAJ"
//      },
//      "saleInfo": {
//        "country": "GB",
//        "saleability": "NOT_FOR_SALE",
//        "isEbook": false
//      },
//      "accessInfo": {
//        "country": "GB",
//        "viewability": "NO_PAGES",
//        "embeddable": false,
//        "publicDomain": false,
//        "textToSpeechPermission": "ALLOWED",
//        "epub": {
//          "isAvailable": false
//        },
//        "pdf": {
//          "isAvailable": true
//        },
//        "webReaderLink": "http://play.google.com/books/reader?id=1GIrEAAAQBAJ&hl=&source=gbs_api",
//        "accessViewStatus": "NONE",
//        "quoteSharingAllowed": false
//      },
//      "searchInfo": {
//        "textSnippet": "As the students are picked off one by one, he weaves in the story of the mainland investigation of the earlier murders. This is a homage to Golden Age detective fiction, but it’s also unabashed entertainment.&quot;"
//      }
//    }
//  ]
//}