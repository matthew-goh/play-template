package controllers

import models.DataModel
import play.api.libs.json._
import play.api.mvc._
import repositories.DataRepository
import services.{LibraryService, RepositoryService}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(repoService: RepositoryService, service: LibraryService, val controllerComponents: ControllerComponents)
                                     (implicit ec: ExecutionContext) extends BaseController {

  def showBookDetails(id: String): Action[AnyContent] = Action.async {implicit request =>
    val badBook = DataModel("Not found", "N/A", "N/A", 0)
    repoService.read(id).map{
      case Right(item) => Ok(views.html.bookdetails(item))
      case Left(error) => BadRequest(views.html.bookdetails(badBook))
    }
//    Future.successful(Ok(views.html.example(bookData)))
  }

  //  display a list of all DataModels in the database, selected without parameters
//  def index() = Action(Ok) // return a 200 OK response to fulfill the test
  def index(): Action[AnyContent] = Action.async { implicit request =>
    repoService.index().map{ // dataRepository.index() is a Future[Either[APIError.BadAPIResponse, Seq[DataModel]]]
      case Right(item: Seq[DataModel]) => Ok {Json.toJson(item)}
      case Left(error) => Status(error.httpResponseStatus)(error.reason)
    }
  }

  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    // validate method checks that the request body contains all the fields with the correct types needed to create a DataModel
    request.body.validate[DataModel] match { // valid or invalid request
      case JsSuccess(dataModel, _) =>
        repoService.create(dataModel).map{
          case Right(_) => Created {request.body}
          case Left(error) => BadRequest {error.reason}
        }
      // dataRepository.create() is a Future[Either[APIError.BadAPIResponse, DataModel]
      case JsError(_) => Future(BadRequest) // ensure correct return type
    }
  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.read(id).map{ // dataRepository.read() is a Future[Either[APIError, DataModel]]
      case Right(item) => Ok {Json.toJson(item)}
      case Left(error) => NotFound {error.reason}
    }
  }
  def readBySpecifiedField(field: String, name: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.readBySpecifiedField(field, name).map{
      case Right(item) => Ok {Json.toJson(item)}
      case Left(error) => BadRequest {error.reason}
    } // dataRepository.readBySpecifiedField() is a Future[Either[APIError, Seq[DataModel]]]
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        repoService.update(id, dataModel).map{
          case Right(_) => Accepted {Json.toJson(request.body)}
          case Left(error) => Status(error.httpResponseStatus)(error.reason)
        } // dataRepository.update() is a Future[Either[APIError, result.UpdateResult]]
      case JsError(_) => Future(BadRequest)
    }
  }
  def updateWithValue(id: String, field: String, newValue: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.updateWithValue(id, field, newValue).map{
      case Right(_) => Accepted {s"$field of book $id has been updated to: $newValue"}
      case Left(error) => BadRequest {error.reason}
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.delete(id).map{
      case Right(_) => Accepted
      case Left(error) => Status(error.httpResponseStatus)(error.reason)
    } // dataRepository.delete() is a Future[Either[APIError, result.DeleteResult]]
  }

//  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
//    service.getGoogleBook(search = search, term = term).value.map {
//      case Right(book) => Ok {Json.toJson(book)}
//      case Left(error) => Status(error.httpResponseStatus)(error.reason)
//    }
//  }
  def getGoogleCollection(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleCollection(search = search, term = term).value.map {
      case Right(collection) => {
        Ok {Json.toJson(collection)}
      }
      case Left(error) => BadRequest {error.reason}
    }
  }
  def getGoogleBookList(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleCollection(search = search, term = term).value.map {
      case Right(collection) => {
        Ok {Json.toJson(service.extractBooksFromCollection(collection))}
      }
      case Left(error) => BadRequest {error.reason}
    }
  }

  def searchBooksAndDisplay(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    // Step 1: get raw search results
    service.getGoogleCollection(search = search, term = term).value.map {
      case Right(collection) => {
        // Step 2: convert to list of DataModels
        val bookList: Seq[DataModel] = service.extractBooksFromCollection(collection)
        // Step 3: add books to database (for ids not already there)
        bookList.map(book => repoService.create(book))
        // Step 4: display the search results on a webpage
        Ok(views.html.searchresults(bookList))
      }
      case Left(error) => BadRequest {error.reason}
//      case Left(error) => BadRequest(views.html.index())
    }
  }
}
