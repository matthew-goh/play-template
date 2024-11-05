package controllers

import play.api.mvc._

import javax.inject._
import scala.concurrent.Future

@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
//  def index() = TODO
  def index() = Action(Ok) // return a 200 OK response to fulfill the test

  def create() = TODO

  def read(id: String) = TODO

  def update(id: String) = TODO

  def delete(id: String) = TODO
}
