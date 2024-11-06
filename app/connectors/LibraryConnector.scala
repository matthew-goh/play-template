package connectors

import play.api.libs.json._
import play.api.libs.ws.WSClient

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class LibraryConnector @Inject()(ws: WSClient) {
  // used for a GET method
  // implicit rds: OFormat[Response] is needed to parse the Json response model as our model
  // [Response] is a type parameter for our method, this must be defined when calling the method
  def get[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): Future[Response] = {
    val request = ws.url(url) // creates our request using the url
    val response = request.get()
    response.map {
      result => result.json.as[Response]
    }
  }
  // Assumptions:
  // expecting a json body in the response
  // json body can be parsed into our model
  // request made was a success
}

// def fetchBookData(query: String): Future[Option[GoogleBooksResponse]] = {
//    ws.url(s"$googleBooksApiUrl?q=$query")
//      .get()
//      .map { response =>
//        response.status match {
//          case 200 =>
//            // Deserialize the JSON response into the case class
//            response.json.asOpt[GoogleBooksResponse]
//          case _ =>
//            // Handle error (non-200 status)
//            None
//        }
//      }
//  }
