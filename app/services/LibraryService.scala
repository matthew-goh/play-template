package services

import cats.data.EitherT
import connectors.LibraryConnector
import models.{APIError, Book, Collection, DataModel, VolumeInfo}
import play.api.libs.json.JsValue

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LibraryService @Inject()(connector: LibraryConnector) {
  // call the connector via this service
  // e.g. https://www.googleapis.com/books/v1/volumes?q=flowers+inauthor
  // e.g. https://www.googleapis.com/books/v1/volumes?q=decagon+isbn:9781782276340
//  def getGoogleBook(urlOverride: Option[String] = None, search: String, term: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, Book] = {
//    connector.get[Book](urlOverride.getOrElse(s"https://www.googleapis.com/books/v1/volumes?q=$search%$term"))
//  }

  def getGoogleCollection(urlOverride: Option[String] = None, search: String, term: String)(implicit ec: ExecutionContext): EitherT[Future, APIError, Collection] = {
    connector.get[Collection](urlOverride.getOrElse(s"https://www.googleapis.com/books/v1/volumes?q=$search%$term"))
  }
  // version called by ApplicationController searchGoogleAndDisplay()
  def getGoogleCollection(reqBody: Option[Map[String, Seq[String]]])(implicit ec: ExecutionContext): EitherT[Future, APIError, Collection] = {
    val search: String = reqBody.flatMap(_.get("search").flatMap(_.headOption)).getOrElse("")
    val keyword: String = reqBody.flatMap(_.get("keyword").flatMap(_.headOption)).getOrElse("")
    val termValue: String = reqBody.flatMap(_.get("term_value").flatMap(_.headOption)).getOrElse("")

    if (keyword == "") EitherT.leftT(APIError.BadAPIResponse(400, "Keyword missing from search"))
    else {
      val term = keyword + ":" + termValue
      connector.get[Collection](s"https://www.googleapis.com/books/v1/volumes?q=$search%$term")
    }
  }

  def extractBooksFromCollection(collection: Collection): Seq[DataModel] = {
    collection.items match {
      case Some(bookList) => bookList.map { book => convertBookToDataModel(book) }
      case None => Seq()
    }
  }

  def convertBookToDataModel(book: Book): DataModel = {
    val volInfo = book.volumeInfo
    val bookDescription = volInfo.description.getOrElse("")
    DataModel(_id = book.id, name = volInfo.title, description = bookDescription, pageCount = volInfo.pageCount)
  }
}
