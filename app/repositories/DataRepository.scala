package repositories

import com.google.inject.ImplementedBy
import models.{APIError, DataModel}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model._
import org.mongodb.scala.{result}
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
    Indexes.ascending("_id")
  )), // can ensure the bookId to be unique
  replaceIndexes = false
) with DataRepositoryTrait {
  // list all DataModels in the database (one DataModel is one book)
  def index(): Future[Either[APIError.BadAPIResponse, Seq[DataModel]]]  = {
    try {
      collection.find().toFuture().map{ books: Seq[DataModel] => Right(books) }
    }
    catch {
      case e: Exception => Future(Left(APIError.BadAPIResponse(404, "Database not found")))
    }
  }

  // add a DataModel object to database
  def create(book: DataModel): Future[Either[APIError.BadAPIResponse, DataModel]] = {
    try {
      collection.insertOne(book).toFuture()
        .map(_ => Right(book))
        .recover { case _ => Left(APIError.BadAPIResponse(500, "Book already exists in database")) }
    }
    catch {
      case e: Exception => Future(Left(APIError.BadAPIResponse(500, "Unable to add book")))
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
      Filters.equal(field, value)
    )

  // retrieve a DataModel object from database - uses an id parameter
  def read(id: String): Future[Either[APIError, DataModel]] = {
    try {
      collection.find(byID(id)).headOption flatMap {
        case Some(data) => Future(Right(data))
        case None => Future(Left(APIError.BadAPIResponse(404, "Book not found")))
      }
    }
    catch {
      case e: Exception => Future(Left(APIError.BadAPIResponse(500, "Unable to search for book")))
    }
  }

  // retrieve a list of DataModel objects for which a specified field equals a specified value
  def readBySpecifiedField(field: String, value: String): Future[Either[APIError, Seq[DataModel]]] = {
    field match {
      case "_id" | "name" | "description" => {
        try {
          collection.find(bySpecifiedField(field, value)).toFuture().map(books => Right(books))
        }
        catch {
          case e: Exception => Future(Left(APIError.BadAPIResponse(500, "Unable to search for books")))
        }
      }
      case _ => Future(Left(APIError.BadAPIResponse(500, "Invalid field to search")))
    }
  }

  // takes in a DataModel, finds a matching document with the same id and updates the document, then returns the updated DataModel
  def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]] = {
    try {
      collection.replaceOne(
        filter = byID(id),
        replacement = book,
        options = new ReplaceOptions().upsert(true) //What happens when we set this to false?
      ).toFuture().map(result => Right(result))
    }
    catch {
      case e: Exception => Future(Left(APIError.BadAPIResponse(500, "Unable to update book")))
    }
  }
  // Right result is e.g. AcknowledgedUpdateResult{matchedCount=1, modifiedCount=1, upsertedId=null}

  def updateWithValue(id: String, field: String, newValue: String): Future[Either[APIError, result.UpdateResult]] = {
    field match {
      case "name" | "description" =>
        try {
          collection.updateOne(Filters.equal("_id", id), Updates.set(field, newValue)).toFuture().map{
              updateResult =>
                if (updateResult.getMatchedCount == 0) Left(APIError.BadAPIResponse(404, "Book not found"))
                else Right(updateResult)
            }
        }
        catch {
          case e: Exception => Future(Left(APIError.BadAPIResponse(500, "Unable to update book")))
        }
      case "pageCount" =>
        if(!newValue.forall(Character.isDigit)) {
          Future(Left(APIError.BadAPIResponse(500, "Page count must be an integer")))
        } else {
          try{
            collection.updateOne(Filters.equal("_id", id), Updates.set(field, newValue.toInt)).toFuture().map{
              updateResult =>
                if (updateResult.getMatchedCount == 0) Left(APIError.BadAPIResponse(404, "Book not found"))
                else Right(updateResult)
            }
          }
          catch {
            case e: Exception => Future(Left(APIError.BadAPIResponse(500, "Unable to update book")))
          }
        }
      case _ => Future(Left(APIError.BadAPIResponse(500, "Invalid field to update")))
    }
//    collection.replaceOne(
//      filter = byID(id),
//      replacement = book,
//      options = new ReplaceOptions().upsert(true) //What happens when we set this to false?
//    ).toFuture()
  }

  // delete a document
  def delete(id: String): Future[Either[APIError, result.DeleteResult]] = {
    try {
      collection.deleteOne(
        filter = byID(id)
      ).toFuture().map(result => Right(result))
    }
    catch {
      case e: Exception => Future(Left(APIError.BadAPIResponse(500, "Unable to delete book")))
    }
  }

  // remove all data from Mongo with the same collection name
  def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ()) // needed for tests

}

@ImplementedBy(classOf[DataRepository])
trait DataRepositoryTrait {
  def index(): Future[Either[APIError.BadAPIResponse, Seq[DataModel]]]
  def create(book: DataModel): Future[Either[APIError.BadAPIResponse, DataModel]]
  def read(id: String): Future[Either[APIError, DataModel]]
  def readBySpecifiedField(field: String, value: String): Future[Either[APIError, Seq[DataModel]]]
  def update(id: String, book: DataModel): Future[Either[APIError, result.UpdateResult]]
  def updateWithValue(id: String, field: String, newValue: String): Future[Either[APIError, result.UpdateResult]]
  def delete(id: String): Future[Either[APIError, result.DeleteResult]]
}