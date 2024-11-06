package services

import baseSpec.BaseSpec
import cats.data.EitherT
import connectors.LibraryConnector
import models.{APIError, Book}
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

  val gameOfThrones: JsValue = Json.obj(
    "_id" -> "someId",
    "name" -> "A Game of Thrones",
    "description" -> "The best book!!!",
    "pageCount" -> 100
  )
  // {"_id":"someId","name":"A Game of Thrones","description":"The best book!!!","pageCount":100}

  "getGoogleBook" should {
    val url: String = "testUrl"

    "return a book" in {
      println(gameOfThrones.as[Book])
      (mockConnector.get[Book](_: String)(_: OFormat[Book], _: ExecutionContext))
        .expects(url, *, *) // can take *, which shows that the connector can expect any request in place of the parameter. You might sometimes see this as any().
        .returning(EitherT.rightT(gameOfThrones.as[Book])) // explicitly states what the connector method returns
        .once() // how many times we can expect this response

      // allows for the result to be waited for as the Future type can be seen as a placeholder for a value we don't have yet
      whenReady(testService.getGoogleBook(urlOverride = Some(url), search = "", term = "").value) { result =>
        result shouldBe Right(Book("someId", "A Game of Thrones", Some("The best book!!!"), 100))
        // Book(someId,A Game of Thrones,Some(The best book!!!),100)
      }
    }

    "return an error" in {
      val url: String = "testUrl"

      (mockConnector.get[Book](_: String)(_: OFormat[Book], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.leftT(APIError.BadAPIResponse(500, "Could not connect")))// How do we return an error?
        .once()

      whenReady(testService.getGoogleBook(urlOverride = Some(url), search = "", term = "").value) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect"))
      }
    }
  }
}
