package repositories

import com.google.inject.ImplementedBy
import models.{APIError, DataModel}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model._
import org.mongodb.scala.{MongoWriteException, result}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
// extends PlayMongoRepository[DataModel] tells the library what the structure of our data looks like by using our newly created DataModel.
// This means that every document inserted into the database has the same structure, with _id, name, description and pageCount properties.
// For this database structure, each document will be identified by the _id.
class DataRepository @Inject()(mongoComponent: MongoComponent)
                              (implicit ec: ExecutionContext) extends PlayMongoRepository[DataModel](
  // required parameters for the PlayMongoRepository abstract class
  collectionName = "dataModels",
  mongoComponent = mongoComponent,
  domainFormat = DataModel.formats, // uses the implicit val formats in the DataModel object
  //-tells the driver how to read and write between a DataModel and JSON
  indexes = Seq(IndexModel(
    Indexes.ascending("_id") // using book ID as MongoDB's unique-valued _id field
  )),
  replaceIndexes = false
) with DataRepositoryTrait {

  // list all DataModels in the database (one DataModel is one book)
  def index(): Future[Either[APIError, Seq[DataModel]]]  = {
    collection.find().toFuture().map{ books: Seq[DataModel] => Right(books) }
      .recover{
        case e: Exception => Left(APIError.BadAPIResponse(500, s"Unable to find database collection: ${e.getMessage}"))
      }
  }

  // add a DataModel object to database
  def create(book: DataModel): Future[Either[APIError, DataModel]] = {
    collection.insertOne(book).toFuture().map { insertResult =>
      if (insertResult.wasAcknowledged) {
        Right(book)
      } else {
        Left(APIError.BadAPIResponse(500, "Error: Insertion not acknowledged"))
      }
    }.recover {
      case e: MongoWriteException => Left(APIError.BadAPIResponse(500, "Book already exists in database"))
      case e: Exception => Left(APIError.BadAPIResponse(500, s"Unable to add book: ${e.getMessage}"))
    }
  }

  private def byID(id: String): Bson =
    Filters.and(
      Filters.equal("_id", id)
    )
//  private def hasField(field: String): Bson =
//    Filters.and(
//      Filters.exists(field)
//    )
  private def bySpecifiedField(field: String, value: String): Bson =
    Filters.and(
        Filters.regex(field, s".*${value}.*", "i") // case-insensitive regex filter, containing search value
    )

  // retrieve a DataModel object from database - uses an id parameter
  def read(id: String): Future[Either[APIError, DataModel]] = {
    collection.find(byID(id)).headOption.flatMap {
      case Some(data) => Future(Right(data))
      case None => Future(Left(APIError.BadAPIResponse(404, "Book not found")))
    }.recover {
      case e: Exception => Left(APIError.BadAPIResponse(500, s"Unable to search for book: ${e.getMessage}"))
    }
  }

  // retrieve a list of DataModel objects for which a specified field contains a specified value
  def readBySpecifiedField(field: DataModelFields.Value, value: String): Future[Either[APIError, Seq[DataModel]]] = {
    field match {
      case DataModelFields._id | DataModelFields.name | DataModelFields.description => {
        collection.find(bySpecifiedField(field.toString, value)).toFuture().map(books => Right(books))
          .recover {
            case e: Exception => Left(APIError.BadAPIResponse(500, s"Unable to search for books: ${e.getMessage}"))
          }
      }
      case DataModelFields.pageCount => Future.successful(Left(APIError.BadAPIResponse(400, "Cannot search page count")))
    }
  }

  // takes in a DataModel, finds a matching document with the same id and updates the document, then returns the updated DataModel
  def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]] = {
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(false) // don't add to database if user doesn't exist
    ).toFuture().map {
      updateResult =>
        if (updateResult.wasAcknowledged) {
          updateResult.getMatchedCount match {
            case 1 => Right(updateResult)
            case 0 => Left(APIError.BadAPIResponse(404, "Book not found"))
            case _ => Left(APIError.BadAPIResponse(500, "Error: Multiple books with same ID found"))
          }
        } else {
          Left(APIError.BadAPIResponse(500, "Error: Update not acknowledged"))
        }
    }.recover {
      case e: Exception => Left(APIError.BadAPIResponse(500, s"Unable to update book: ${e.getMessage}"))
    }
  }
  // updateResult is e.g. AcknowledgedUpdateResult{matchedCount=1, modifiedCount=1, upsertedId=null}

  private def isIntegerString(value: String): Boolean = value.forall(Character.isDigit)
  def updateWithValue(id: String, field: DataModelFields.Value, newValue: String): Future[Either[APIError, result.UpdateResult]] = {
    field match {
      case DataModelFields.name | DataModelFields.description =>
        collection.updateOne(Filters.equal("_id", id), Updates.set(field.toString, newValue)).toFuture().map{
          updateResult =>
            if (updateResult.getMatchedCount == 0) Left(APIError.BadAPIResponse(404, "Book not found"))
            else Right(updateResult)
        }.recover {
          case e: Exception => Left(APIError.BadAPIResponse(500, s"Unable to update book: ${e.getMessage}"))
        }
      case DataModelFields.pageCount =>
        if (isIntegerString(newValue)) {
          collection.updateOne(Filters.equal("_id", id), Updates.set("pageCount", newValue.toInt)).toFuture().map{
            updateResult =>
              if (updateResult.getMatchedCount == 0) Left(APIError.BadAPIResponse(404, "Book not found"))
              else Right(updateResult)
          }.recover {
            case e: Exception => Left(APIError.BadAPIResponse(500, s"Unable to update book: ${e.getMessage}"))
          }
        } else {
          Future.successful(Left(APIError.BadAPIResponse(400, "Page count must be an integer")))
        }
      case DataModelFields._id => Future.successful(Left(APIError.BadAPIResponse(400, "Cannot update book ID")))
    }
  }

  // delete a document
  def delete(id: String): Future[Either[APIError, result.DeleteResult]] = {
    collection.deleteOne(
      filter = byID(id)
    ).toFuture().map { deleteResult =>
      if (deleteResult.wasAcknowledged) {
        deleteResult.getDeletedCount match {
          case 1 => Right(deleteResult)
          case 0 => Left(APIError.BadAPIResponse(404, "Book not found"))
          case _ => Left(APIError.BadAPIResponse(500, "Error: Multiple books deleted"))
        }
      } else {
        Left(APIError.BadAPIResponse(500, "Error: Delete not acknowledged"))
      }
    }.recover {
      case e: Exception => Left(APIError.BadAPIResponse(500, s"Unable to delete book: ${e.getMessage}"))
    }
  }

  // remove all data from Mongo with the same collection name
  def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ()) // needed for tests

}

@ImplementedBy(classOf[DataRepository])
trait DataRepositoryTrait {
  def index(): Future[Either[APIError, Seq[DataModel]]]
  def create(book: DataModel): Future[Either[APIError, DataModel]]
  def read(id: String): Future[Either[APIError, DataModel]]
  def readBySpecifiedField(field: DataModelFields.Value, value: String): Future[Either[APIError, Seq[DataModel]]]
  def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]]
  def updateWithValue(id: String, field: DataModelFields.Value, newValue: String): Future[Either[APIError, result.UpdateResult]]
  def delete(id: String): Future[Either[APIError, result.DeleteResult]]
}

object DataModelFields extends Enumeration {
  val _id, name, description, pageCount = Value
}
