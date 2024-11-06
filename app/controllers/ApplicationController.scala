package controllers

import models.DataModel
import play.api.libs.json._
import play.api.mvc._
import repositories.DataRepository
import services.LibraryService

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(dataRepository: DataRepository, service: LibraryService, val controllerComponents: ControllerComponents)
                                     (implicit ec: ExecutionContext) extends BaseController {

//  display a list of all DataModels in the database, selected without parameters
//  def index() = Action(Ok) // return a 200 OK response to fulfill the test
  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.index().map{ // dataRepository.index() is a Future[Either[Int, Seq[DataModel]]]
      case Right(item: Seq[DataModel]) => Ok {Json.toJson(item)}
      case Left(error) => Status(error)(Json.toJson("Unable to find any books"))
    }
  }

  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    // validate method checks that the request body contains all the fields with the correct types needed to create a DataModel
    request.body.validate[DataModel] match { // valid or invalid request
      case JsSuccess(dataModel, _) =>
        dataRepository.create(dataModel).map(_ => Created)
      // dataRepository.create() is a Future[DataModel]
      case JsError(_) => Future(BadRequest) // ensure correct return type
    }
  }
  // How would you check something is created in the database?
  //Think about what is returned from .create() and how you can match on this similar to validating the request body

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.read(id).map{ // dataRepository.index() is a Future[DataModel]
      item => Ok {Json.toJson(item)}
    }
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.update(id, dataModel).map(_ => Accepted {Json.toJson(request.body)})
      // dataRepository.update(id, dataModel).map(_ => Accepted {this.read(id)}) // ???
      // dataRepository.update() is a Future[result.UpdateResult]
      case JsError(_) => Future(BadRequest)
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.delete(id).map(_ => Accepted)
    // dataRepository.delete() is a Future[result.DeleteResult]
  }

  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleBook(search = search, term = term).map {
      item => Ok {Json.toJson(item)}
    }
  }
}

// googleBooksService.fetchBookData("Scala Programming").map {
//  case Some(book) =>
//    // Successfully fetched book data
//    println(s"Book Title: ${book.volumeInfo.title}")
//    book.volumeInfo.authors.foreach(authors => println(s"Authors: ${authors.mkString(", ")}"))
//  case None =>
//    // Handle error (e.g., book not found)
//    println("No book found or error occurred.")
//}
