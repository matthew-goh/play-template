package services

import cats.data.EitherT
import models.{APIError, DataModel}
import org.mongodb.scala.result
import repositories.{DataRepository, DataRepositoryTrait}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

// (implicit ec: ExecutionContext) can be at the class level or method level; needed for futures and mapping
class RepositoryService @Inject()(repositoryTrait: DataRepositoryTrait){

  def index(): Future[Either[APIError.BadAPIResponse, Seq[DataModel]]] = {
    repositoryTrait.index()
  }

  def create(book: DataModel): Future[Either[APIError.BadAPIResponse, DataModel]] = {
    repositoryTrait.create(book)
  }
  // version called by ApplicationController addFromSearch()
  def create(reqBody: Option[Map[String, Seq[String]]]): Future[Either[APIError.BadAPIResponse, DataModel]] = {
    val id: String = reqBody.flatMap(_.get("_id").flatMap(_.headOption)).getOrElse("")
    val name: String = reqBody.flatMap(_.get("name").flatMap(_.headOption)).getOrElse("")
    val description: String = reqBody.flatMap(_.get("description").flatMap(_.headOption)).getOrElse("")
    val pageCount: Int = reqBody.flatMap(_.get("pageCount").flatMap(_.headOption)).getOrElse("0").toInt

    val book = DataModel(id, name, description, pageCount)
    repositoryTrait.create(book)
  }

  def read(id: String): Future[Either[APIError, DataModel]] = {
    repositoryTrait.read(id)
  }

  // retrieve a list of DataModel objects for which a specified field equals a specified value
  def readBySpecifiedField(field: String, value: String): Future[Either[APIError, Seq[DataModel]]] = {
    repositoryTrait.readBySpecifiedField(field, value)
  }

  // takes in a DataModel, finds a matching document with the same id and updates the document, then returns the updated DataModel
  def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]] = {
    repositoryTrait.update(id, book)
  }

  def updateWithValue(id: String, field: String, newValue: String): Future[Either[APIError, result.UpdateResult]] = {
    repositoryTrait.updateWithValue(id, field, newValue)
  }

  // delete a document
  def delete(id: String): Future[Either[APIError, result.DeleteResult]] = {
    repositoryTrait.delete(id)
  }
}
