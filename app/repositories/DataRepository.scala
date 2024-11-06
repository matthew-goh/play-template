package repositories

import models.{APIError, DataModel}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model._
import org.mongodb.scala.result
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
) {
  // list all DataModels in the database (one DataModel is one book)
  def index(): Future[Either[APIError.BadAPIResponse, Seq[DataModel]]]  =
    collection.find().toFuture().map{
      case books: Seq[DataModel] => Right(books)
      case _ => Left(APIError.BadAPIResponse(404, "Books cannot be found"))
    }

  // add a DataModel object to database
  def create(book: DataModel): Future[DataModel] =
    collection
      .insertOne(book)
      .toFuture()
      .map(_ => book)

  private def byID(id: String): Bson =
    Filters.and(
      Filters.equal("_id", id)
    )

  // retrieve a DataModel object from database - uses an id parameter
  def read(id: String): Future[DataModel] =
    collection.find(byID(id)).headOption flatMap {
      case Some(data) =>
        Future(data)
    }

  // takes in a DataModel, finds a matching document with the same id and updates the document, then returns the updated DataModel
  def update(id: String, book: DataModel): Future[result.UpdateResult] =
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(true) //What happens when we set this to false?
    ).toFuture()

  // delete a document
  def delete(id: String): Future[result.DeleteResult] =
    collection.deleteOne(
      filter = byID(id)
    ).toFuture()

  // remove all data from Mongo with the same collection name
  def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ()) //Hint: needed for tests

}
