package controllers

import javax.inject.Inject

import controllers.Models.AdminData
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import views.html

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by zh on 16-11-13.
  */
class Login @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport{



  val userForm = Form(
    mapping(
      "name" -> text,
      "password" -> text
    )(AdminData.apply)(AdminData.unapply)
  )

  def index = Action {
    Ok(views.html.login(userForm))
  }

  def authenticate = Action { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.index()),
      user           => Redirect(routes.Application.index)
    )
  }

}
