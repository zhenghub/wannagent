package controllers

import jp.t2v.lab.play2.auth.LoginLogout
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views.html

import scala.concurrent.Future

/**
  * Created by zh on 16-11-11.
  */
class Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

}