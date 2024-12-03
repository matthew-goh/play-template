package services

import cats.data.EitherT
import models.{APIError, DataModel}
import org.mongodb.scala.result
import repositories.{DataModelFields, DataRepositoryTrait}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

// (implicit ec: ExecutionContext) can be at the class level or method level; needed for futures and mapping
class RepositoryService @Inject()(repositoryTrait: DataRepositoryTrait)
                                 (implicit ec: ExecutionContext){

  def index(): Future[Either[APIError.BadAPIResponse, Seq[DataModel]]] = {
    repositoryTrait.index()
  }

  def create(book: DataModel): Future[Either[APIError.BadAPIResponse, DataModel]] = {
    repositoryTrait.create(book)
  }
  // version called by ApplicationController addFromSearch()
  def create(reqBody: Option[Map[String, Seq[String]]]): Future[Either[APIError.BadAPIResponse, DataModel]] = {
//    val id: String = reqBody.flatMap(_.get("_id").flatMap(_.headOption)).getOrElse("")
//    val name: String = reqBody.flatMap(_.get("name").flatMap(_.headOption)).getOrElse("")
//    val description: String = reqBody.flatMap(_.get("description").flatMap(_.headOption)).getOrElse("")
//    val pageCount: Int = reqBody.flatMap(_.get("pageCount").flatMap(_.headOption)).getOrElse("0").toInt

    val missingError = APIError.BadAPIResponse(400, "Missing required value")
    val invalidTypeError = APIError.BadAPIResponse(400, "Invalid data type")

    // description can be blank
    val description: String = reqBody.flatMap(_.get("description").flatMap(_.headOption)).getOrElse("")
    val reqBodyValuesEither: Either[APIError.BadAPIResponse, DataModel] = for {
      // if any required value is missing, the result is Left(missingError)
      id <- reqBody.flatMap(_.get("_id").flatMap(_.headOption)).toRight(missingError)
      name <- reqBody.flatMap(_.get("name").flatMap(_.headOption)).toRight(missingError)
      pageCountStr <- reqBody.flatMap(_.get("pageCount").flatMap(_.headOption)).toRight(missingError)
      // if any data type is invalid, the result is Left(invalidTypeError)
      pageCount <- Try(pageCountStr.toInt).toOption.toRight(invalidTypeError)
    } yield DataModel(id, name, description, pageCount)

    reqBodyValuesEither match {
      case Right(book) => repositoryTrait.create(book)
      case Left(error) => Future(Left(error))
    }
  }

  def read(id: String): Future[Either[APIError, DataModel]] = {
    repositoryTrait.read(id)
  }

  // retrieve a list of DataModel objects for which a specified field equals a specified value
  def readBySpecifiedField(field: String, value: String): Future[Either[APIError, Seq[DataModel]]] = {
    val fieldTry: Try[DataModelFields.Value] = Try(DataModelFields.withName(field))
    fieldTry match {
      case Success(fieldName) => repositoryTrait.readBySpecifiedField(fieldName, value)
      case Failure(e) => Future(Left(APIError.BadAPIResponse(400, "Invalid field to search")))
    }
  }

  // takes in a DataModel, finds a matching document with the same id and updates the document, then returns the updated DataModel
  def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]] = {
    repositoryTrait.update(id, book)
  }

  def updateWithValue(id: String, field: String, newValue: String): Future[Either[APIError, result.UpdateResult]] = {
    val fieldTry: Try[DataModelFields.Value] = Try(DataModelFields.withName(field))
    fieldTry match {
      case Success(fieldName) => repositoryTrait.updateWithValue(id, fieldName, newValue)
      case Failure(e) => Future(Left(APIError.BadAPIResponse(400, "Invalid field to update")))
    }
  }

  // delete a document
  def delete(id: String): Future[Either[APIError, result.DeleteResult]] = {
    repositoryTrait.delete(id)
  }
}
