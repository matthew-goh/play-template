package services

import cats.data.EitherT
import connectors.LibraryConnector
import models.{APIError, Book}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LibraryService @Inject()(connector: LibraryConnector) {
  // call the connector via this service
  def getGoogleBook(urlOverride: Option[String] = None, search: String, term: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, Book] = {
    connector.get[Book](urlOverride.getOrElse(s"https://www.googleapis.com/books/v1/volumes?q=$search%$term"))
    // e.g. https://www.googleapis.com/books/v1/volumes?q=flowers+inauthor
  }
}
