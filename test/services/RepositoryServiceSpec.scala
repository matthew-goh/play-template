package services

import baseSpec.BaseSpec
import com.mongodb.client.result._
import eu.timepit.refined.auto._
import models.{APIError, DataModel}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import repositories.{DataModelFields, DataRepositoryTrait}

import scala.concurrent.{ExecutionContext, Future}

class RepositoryServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {
//  val mockMongoComponent: MongoComponent = mock[MongoComponent]
//  val mockRepository: DataRepository = new DataRepository(mockMongoComponent)

  val mockRepoTrait: DataRepositoryTrait = mock[DataRepositoryTrait]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testRepoService = new RepositoryService(mockRepoTrait)

  private val dataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )
  private val newDataModel: DataModel = DataModel(
    "abcd",
    "new name",
    "new description",
    200
  )
  private val dataModel2: DataModel = DataModel(
    "123",
    "Decagon",
    "best book",
    100
  )
  private val testUpdateResult: UpdateResult = UpdateResult.acknowledged(1, 1, null)
  private val testDeleteResult: DeleteResult = DeleteResult.acknowledged(1)

  "index" should {
    "return a list of DataModels" in {
      (mockRepoTrait.index _)
        .expects()
        .returning(Future(Right(Seq(dataModel, dataModel2)))) // explicitly states what the connector method returns
        .once() // how many times we can expect this response

//      val result = testRepoService.index()
//      result.futureValue shouldBe Right(Seq(dataModel, dataModel2))

      // allows for the result to be waited for as the Future type can be seen as a placeholder for a value we don't have yet
      // -no need .value since output is a Future, not EitherT
      whenReady(testRepoService.index()) { result =>
        result shouldBe Right(Seq(dataModel, dataModel2))
      }
    }

    "return an error" in {
      (mockRepoTrait.index _)
        .expects()
        .returning(Future(Left(APIError.BadAPIResponse(500, "Unable to find database collection"))))
        .once()

        whenReady(testRepoService.index()) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Unable to find database collection"))
      }
    }
  }

  "create" should {
    "return a DataModel" in {
      (mockRepoTrait.create(_: DataModel))
        .expects(dataModel)
        .returning(Future(Right(dataModel)))
        .once()

      whenReady(testRepoService.create(dataModel)) { result =>
        result shouldBe Right(dataModel)
      }
    }

    "return an error" in {
      (mockRepoTrait.create(_: DataModel))
        .expects(*)
        .returning(Future(Left(APIError.BadAPIResponse(500, "Unable to add book"))))
        .once()

      whenReady(testRepoService.create(dataModel)) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Unable to add book"))
      }
    }
  }

  "create (version called by ApplicationController addFromSearch())" should {
    "return a DataModel" in {
      val reqBody = Some(Map(
        "_id" -> List("abcd"),
        "name" -> List("test name"),
        "description" -> List("test description"),
        "pageCount" -> List("100")
      ))

      (mockRepoTrait.create(_: DataModel))
        .expects(dataModel)
        .returning(Future(Right(dataModel)))
        .once()

      whenReady(testRepoService.create(reqBody)) { result =>
        result shouldBe Right(dataModel)
      }
    }

    "return an error from DataRepository" in {
      val reqBody = Some(Map(
        "_id" -> List("abcd"),
        "name" -> List("test name"),
        "description" -> List("test description"),
        "pageCount" -> List("100")
      ))

      (mockRepoTrait.create(_: DataModel))
        .expects(*)
        .returning(Future(Left(APIError.BadAPIResponse(500, "Book already exists in database"))))
        .once()

      whenReady(testRepoService.create(reqBody)) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Book already exists in database"))
      }
    }

    "return an error if a required value is missing" in {
      val reqBody = Some(Map(
        "_id" -> List(),
        "name" -> List("test name"),
        "description" -> List("test description"),
        "pageCount" -> List("100")
      ))

      whenReady(testRepoService.create(reqBody)) { result =>
        result shouldBe Left(APIError.BadAPIResponse(400, "Missing required value"))
      }
    }

    "return an error if an incorrect data type is provided" in {
      val reqBody = Some(Map(
        "_id" -> List("abcd"),
        "name" -> List("test name"),
        "description" -> List("test description"),
        "pageCount" -> List("1xx")
      ))

      whenReady(testRepoService.create(reqBody)) { result =>
        result shouldBe Left(APIError.BadAPIResponse(400, "Invalid data type"))
      }
    }
  }


  "read" should {
    "return a DataModel" in {
      (mockRepoTrait.read(_: String))
        .expects(*)
        .returning(Future(Right(dataModel)))
        .once()

      whenReady(testRepoService.read("abcd")) { result =>
        result shouldBe Right(dataModel)
      }
    }

    "return an error" in {
      (mockRepoTrait.read(_: String))
        .expects(*)
        .returning(Future(Left(APIError.BadAPIResponse(404, "Book not found"))))
        .once()

      whenReady(testRepoService.read("abcd")) { result =>
        result shouldBe Left(APIError.BadAPIResponse(404, "Book not found"))
      }
    }
  }

  "readBySpecifiedField" should {
    "return a list of DataModels" in {
      (mockRepoTrait.readBySpecifiedField(_: DataModelFields.Value, _: String))
        .expects(DataModelFields.name, "test name")
        .returning(Future(Right(Seq(dataModel))))
        .once()

      whenReady(testRepoService.readBySpecifiedField("name", "test name")) { result =>
        result shouldBe Right(Seq(dataModel))
      }
    }

    "return an error from DataRepository" in {
      (mockRepoTrait.readBySpecifiedField(_: DataModelFields.Value, _: String))
        .expects(DataModelFields.description, "abc")
        .returning(Future(Left(APIError.BadAPIResponse(500, "Unable to search for books"))))
        .once()

      whenReady(testRepoService.readBySpecifiedField("description", "abc")) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Unable to search for books"))
      }
    }

    "return an error if an invalid field is provided" in {
      whenReady(testRepoService.readBySpecifiedField("bad field", "123")) { result =>
        result shouldBe Left(APIError.BadAPIResponse(400, "Invalid field to search"))
      }
    }
  }

  "update" should {
    "return an UpdateResult" in {
      (mockRepoTrait.update(_: String, _: DataModel))
        .expects(*, *)
        .returning(Future(Right(testUpdateResult)))
        .once()

      whenReady(testRepoService.update("abcd", dataModel)) { result =>
        result shouldBe Right(testUpdateResult)
      }
    }

    "return an error" in {
      (mockRepoTrait.update(_: String, _: DataModel))
        .expects(*, *)
        .returning(Future(Left(APIError.BadAPIResponse(500, "Unable to update book"))))
        .once()

      whenReady(testRepoService.update("abcd", dataModel)) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Unable to update book"))
      }
    }
  }

  "updateWithValue" should {
    "return an UpdateResult" in {
      (mockRepoTrait.updateWithValue(_: String, _: DataModelFields.Value, _: String))
        .expects("abcd", DataModelFields.pageCount, "250")
        .returning(Future(Right(testUpdateResult)))
        .once()

      whenReady(testRepoService.updateWithValue("abcd", "pageCount", "250")) { result =>
        result shouldBe Right(testUpdateResult)
      }
    }

    "return an error from DataRepository" in {
      (mockRepoTrait.updateWithValue(_: String, _: DataModelFields.Value, _: String))
        .expects("abcd", DataModelFields._id, "new")
        .returning(Future(Left(APIError.BadAPIResponse(400, "Cannot update book ID"))))
        .once()

      whenReady(testRepoService.updateWithValue("abcd", "_id", "new")) { result =>
        result shouldBe Left(APIError.BadAPIResponse(400, "Cannot update book ID"))
      }
    }

    "return an error if an invalid field is provided" in {
      whenReady(testRepoService.updateWithValue("abcd", "pgCount", "250")) { result =>
        result shouldBe Left(APIError.BadAPIResponse(400, "Invalid field to update"))
      }
    }
  }

  "delete" should {
    "return a DeleteResult" in {
      (mockRepoTrait.delete(_: String))
        .expects(*)
        .returning(Future(Right(testDeleteResult)))
        .once()

      whenReady(testRepoService.delete("abcd")) { result =>
        result shouldBe Right(testDeleteResult)
      }
    }

    "return an error" in {
      (mockRepoTrait.delete(_: String))
        .expects(*)
        .returning(Future(Left(APIError.BadAPIResponse(500, "Unable to delete book"))))
        .once()

      whenReady(testRepoService.delete("abcd")) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Unable to delete book"))
      }
    }
  }
}
