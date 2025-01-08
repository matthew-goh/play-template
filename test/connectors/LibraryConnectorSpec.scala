package connectors

import baseSpec.BaseSpecWithApplication
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import eu.timepit.refined.auto._
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
          .withBody(LibraryServiceSpec.testAPIResultStr)))

      whenReady(TestLibraryConnector.get[Collection](s"http://$Host:$Port/getgooglecollection/decagon/isbn:9781782276340").value) { result =>
        result shouldBe Right(Collection("books#volumes", 1, Some(Seq(Book("1GIrEAAAQBAJ", LibraryServiceSpec.testAPIVolumeInfo)))))
      }
    }

    "return a Right(Collection) without books" in {
      stubFor(get(urlEqualTo("/getgooglecollection/decagon/isbn:123"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody("""{"kind": "books#volumes", "totalItems": 0}""")))

      whenReady(TestLibraryConnector.get[Collection](s"http://$Host:$Port/getgooglecollection/decagon/isbn:123").value) { result =>
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

      whenReady(TestLibraryConnector.get[Collection](s"http://$Host:$Port/getgooglecollection/decagon/isbn:123").value) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect"))
      }
    }
  }
}