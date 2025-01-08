package controllers

import eu.timepit.refined.auto._
import models.DataModel
import play.api.libs.json._
import play.api.mvc._
import play.filters.csrf.CSRF
import services.{LibraryService, RepositoryService}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(repoService: RepositoryService, service: LibraryService, val controllerComponents: ControllerComponents)
                                     (implicit ec: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {
  ///// METHODS CALLED BY FRONTEND /////
  def accessToken()(implicit request: Request[_]): Option[CSRF.Token] = {
    CSRF.getToken
  }

  def invalidRoute(path: String): Action[AnyContent] = Action.async {_ =>
    Future.successful(NotFound(views.html.pagenotfound(path)))
  }

  def listAllBooks(): Action[AnyContent] = Action.async {implicit request =>
    repoService.index().map{ // dataRepository.index() is a Future[Either[APIError.BadAPIResponse, Seq[DataModel]]]
      case Right(bookList: Seq[DataModel]) => Ok(views.html.listing(bookList))
      case Left(error) => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
    }
  }
  //    val itemsPerPage = 5
  //    val numBooks = bookList.length
  //    val numPages = (numBooks / itemsPerPage) + (if (numBooks % itemsPerPage > 0) 1 else 0)

  def showBookDetails(id: String): Action[AnyContent] = Action.async {implicit request =>
    val badBook = DataModel("Not found", "N/A", "N/A", 0)
    repoService.read(id).map{
      case Right(book) => Ok(views.html.bookdetails(book))
      case Left(error) => error.httpResponseStatus match{
        case 404 => NotFound(views.html.bookdetails(badBook))
        case _ => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
      }
    }
  }
  def searchBookByID(): Action[AnyContent] = Action.async {implicit request =>
    accessToken()
    val idToSearch: Option[String] = request.body.asFormUrlEncoded.flatMap(_.get("bookID").flatMap(_.headOption))
    idToSearch match {
      case None | Some("") => Future.successful(BadRequest(views.html.unsuccessful("No ID provided")))
      case Some(id) => Future.successful(Redirect(routes.ApplicationController.showBookDetails(id)))
    }
  }

  def searchBookByTitle(): Action[AnyContent] = Action.async {implicit request =>
    accessToken()
    val titleToSearch: Option[String] = request.body.asFormUrlEncoded.flatMap(_.get("title").flatMap(_.headOption))
    titleToSearch match {
      case None | Some("") => Future.successful(BadRequest(views.html.unsuccessful("No title provided")))
      case Some(title) => {
        repoService.readBySpecifiedField("name", title).map{
          case Right(books) => Ok(views.html.listing(books))
          case Left(error) => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
        }
      }
    }
  }

  def searchGoogleAndDisplay(): Action[AnyContent] = Action.async { implicit request =>
    accessToken()
    val addToDatabase: String = request.body.asFormUrlEncoded.flatMap(_.get("add_to_database").flatMap(_.headOption)).getOrElse("false")
    // Step 1: get raw search results
    service.getGoogleCollection(request.body.asFormUrlEncoded).value.map {
      case Right(collection) => {
        // Step 2: convert to list of DataModels
        val bookList: Seq[DataModel] = service.extractBooksFromCollection(collection)
        // Step 3: add books to database (for ids not already there)
        if (addToDatabase == "true") bookList.map(book => repoService.create(book))
        // Step 4: display the search results on a webpage
        Ok(views.html.searchresults(bookList, addedToDatabase = addToDatabase == "true"))
      }
      case Left(error) => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
    }
  }
//  def searchGoogleAndDisplay(): Action[AnyContent] = Action.async { implicit request =>
//    accessToken
//    // Step 0: process submitted search
//    val search: String = request.body.asFormUrlEncoded.flatMap(_.get("search").flatMap(_.headOption)).getOrElse("")
//    val keyword: String = request.body.asFormUrlEncoded.flatMap(_.get("keyword").flatMap(_.headOption)).getOrElse("")
//    val termValue: String = request.body.asFormUrlEncoded.flatMap(_.get("term_value").flatMap(_.headOption)).getOrElse("")
//    val addToDatabase: String = request.body.asFormUrlEncoded.flatMap(_.get("add_to_database").flatMap(_.headOption)).getOrElse("false")
//
//    if (keyword == "") Future.successful(BadRequest(views.html.searchresults(Seq(), addedToDatabase = false)))
//    else {
//      // Step 1: get raw search results
//      val term = keyword + ":" + termValue
//      service.getGoogleCollection(search = search, term = term).value.map {
//        case Right(collection) => {
//          // Step 2: convert to list of DataModels
//          val bookList: Seq[DataModel] = service.extractBooksFromCollection(collection)
//          // Step 3: add books to database (for ids not already there)
//          if (addToDatabase == "true") bookList.map(book => repoService.create(book))
//          // Step 4: display the search results on a webpage
//          Ok(views.html.searchresults(bookList, addedToDatabase = addToDatabase == "true"))
//        }
//        case Left(error) => BadRequest(views.html.index())
//      }
//    }
//  }
  def addFromSearch(): Action[AnyContent] = Action.async {implicit request =>
    accessToken()
    repoService.create(request.body.asFormUrlEncoded).map{
      case Right(book) => Ok(views.html.bookdetails(book))
      case Left(error) => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
    }
  }

  // access add book form
  def addBook(): Action[AnyContent] = Action.async {implicit request =>
    Future.successful(Ok(views.html.add(DataModel.dataForm)))
  }
  // called when add book form is submitted
  def addBookForm(): Action[AnyContent] =  Action.async {implicit request =>
    accessToken() //call the accessToken method
    DataModel.dataForm.bindFromRequest().fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        //here write what you want to do if the form has errors
        Future.successful(BadRequest(views.html.add(formWithErrors)))
      },
      formData => {  // formData is already a DataModel
        //here write how you would use this data to create a new book (DataModel)
        repoService.create(formData).map{
          case Right(_) => Ok(views.html.bookdetails(formData))
          case Left(error) => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
        }
      }
    )
  }

  // access update book form
  def updateBook(id: String): Action[AnyContent] = Action.async {implicit request =>
    repoService.read(id).map {
      case Right(book) => {
        val filledForm = DataModel.dataForm.fill(book)  // Pre-fill the form with data from the database
        Ok(views.html.update(filledForm))  // Render the form in the view
      }
      case Left(error) => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
    }
  }
  def updateBookForm(): Action[AnyContent] =  Action.async {implicit request =>
    accessToken() //call the accessToken method
    DataModel.dataForm.bindFromRequest().fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        Future.successful(BadRequest(views.html.update(formWithErrors)))
      },
      formData => {  // formData is already a DataModel
        repoService.update(formData._id, formData).map{
          case Right(_) => Ok(views.html.bookdetails(formData))
          case Left(error) => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
        }
      }
    )
  }

  def deleteBook(id: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.delete(id).map{
      case Right(_) => Ok(views.html.confirmation("Delete"))
      case Left(error) => Status(error.httpResponseStatus)(views.html.unsuccessful(error.reason))
    }
  }


  ///// API METHODS WITHOUT FRONTEND /////
  //  display a list of all DataModels in the database, selected without parameters
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
          case Left(error) => Status(error.httpResponseStatus)(error.reason)
        }
      // dataRepository.create() is a Future[Either[APIError.BadAPIResponse, DataModel]
      case JsError(_) => Future.successful(BadRequest {"Invalid request body"}) // ensure correct return type
    }
  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.read(id).map{ // dataRepository.read() is a Future[Either[APIError, DataModel]]
      case Right(item) => Ok {Json.toJson(item)}
      case Left(error) => Status(error.httpResponseStatus)(error.reason)
    }
  }
  def readBySpecifiedField(field: String, name: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.readBySpecifiedField(field, name).map{
      case Right(item) => Ok {Json.toJson(item)}
      case Left(error) => Status(error.httpResponseStatus)(error.reason)
    } // dataRepository.readBySpecifiedField() is a Future[Either[APIError, Seq[DataModel]]]
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        repoService.update(id, dataModel).map{
          case Right(_) => Accepted {Json.toJson(request.body)}
          case Left(error) => Status(error.httpResponseStatus)(error.reason)
        } // dataRepository.update() is a Future[Either[APIError, result.UpdateResult]]
      case JsError(_) => Future.successful(BadRequest {"Invalid request body"})
    }
  }
  def updateWithValue(id: String, field: String, newValue: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.updateWithValue(id, field, newValue).map{
      case Right(_) => Accepted {s"$field of book $id has been updated to: $newValue"}
      case Left(error) => Status(error.httpResponseStatus)(error.reason)
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.delete(id).map{
      case Right(_) => Accepted {s"Book $id has been deleted"}
      case Left(error) => Status(error.httpResponseStatus)(error.reason)
    } // dataRepository.delete() is a Future[Either[APIError, result.DeleteResult]]
  }

  def getGoogleCollection(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleCollection(search = search, term = term).value.map {
      case Right(collection) => Ok {Json.toJson(collection)}
      case Left(error) => Status(error.httpResponseStatus)(error.reason)
    }
  }
  def getGoogleBookList(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleCollection(search = search, term = term).value.map {
      case Right(collection) => Ok {Json.toJson(service.extractBooksFromCollection(collection))}
      case Left(error) => Status(error.httpResponseStatus)(error.reason)
    }
  }
}
