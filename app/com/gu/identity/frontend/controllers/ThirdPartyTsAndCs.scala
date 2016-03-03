package com.gu.identity.frontend.controllers

import com.gu.identity.cookie.IdentityCookieDecoder
import com.gu.identity.frontend.authentication.UserAuthenticatedActionBuilder._
import com.gu.identity.frontend.configuration.Configuration
import com.gu.identity.frontend.models.{GroupCode, ClientID, ReturnUrl}
import com.gu.identity.frontend.services.{ServiceError, IdentityService}
import com.gu.identity.service.client.models.User
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.HttpErrorHandler
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc._
import com.gu.identity.frontend.logging.Logging
import com.gu.identity.frontend.views.ViewRenderer.renderTsAndCs

import scala.concurrent.{Future, ExecutionContext}

case class AddUserToGroupRequest(groupCode: String, returnUrl: Option[String])

class ThirdPartyTsAndCs(identityService: IdentityService, identityCookieDecoder: IdentityCookieDecoder, config: Configuration, val messagesApi: MessagesApi, httpErrorHandler: HttpErrorHandler) extends Controller with Logging with I18nSupport {

  implicit lazy val executionContext: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def confirmAction[A](group: String, returnUrl: Option[String], clientId: Option[String], skipConfirmation: Option[Boolean]) = UserAuthenticatedAction(identityCookieDecoder).async { request => {
      val clientIdActual = ClientID(clientId)
      val groupCode = GroupCode(group)
      val sc_gu_uCookie = request.scGuUCookie
      val verifiedReturnUrl = ReturnUrl(returnUrl, request.headers.get("Referer"), config)
      val skipConfirmationActual = skipConfirmation.getOrElse(false)

      groupCode match {
        case Some(validGroup) => {
          confirm(validGroup, verifiedReturnUrl, clientIdActual, skipConfirmationActual, sc_gu_uCookie).flatMap {
            case Right(result) => Future.successful(result)
            case Left(errors) => httpErrorHandler.onClientError(request, BAD_REQUEST, "Could not check user's group membership status")
          }
        }
        case None => {
          logger.info(s"Received invalid group code $group")
          httpErrorHandler.onClientError(request, NOT_FOUND, "Invalid Group Code")
        }
      }
    }
  }

  def confirm(groupCode: GroupCode, returnUrl: ReturnUrl, clientId: Option[ClientID], skipConfirmation: Boolean, userCookie: Cookie): Future[Either[Seq[ServiceError], Result]] = {
    checkUserForGroupMembership(groupCode, userCookie).flatMap {
      case Right(true) => Future.successful(Right(SeeOther(returnUrl.url)))
      case Right(false) if skipConfirmation => addToGroup(groupCode, userCookie, returnUrl)
      case Right(false) => Future.successful(Right(renderTsAndCs(config, clientId, groupCode, returnUrl)))
      case Left(errors) => {
        logger.warn(s"Could not check user's group membership status {}", errors)
        Future.successful(Left(errors))
      }
    }
  }

  def addToGroupAction(): Action[AnyContent] = UserAuthenticatedAction(identityCookieDecoder).async { implicit request =>
    val sc_gu_uCookie = request.scGuUCookie

    addUserToGroupRequestFormBody.bindFromRequest.fold(
      errorForm => httpErrorHandler.onClientError(request, BAD_REQUEST, "Invalid form submission"),

      successForm => {
        val verifiedReturnUrl = ReturnUrl(successForm.returnUrl, config)
        val groupCode = GroupCode(successForm.groupCode)
        groupCode match {
          case Some(code) => {
            addToGroup(code, sc_gu_uCookie, verifiedReturnUrl).flatMap{
              case Right(result) => Future.successful(result)
              case Left(errors) => httpErrorHandler.onClientError(request, BAD_REQUEST, "Could not check user's group membership status")
            }
          }
          case None => httpErrorHandler.onClientError(request, NOT_FOUND, "Invalid Group Code")
        }
      }
    )
  }

  def addToGroup(group: GroupCode, sc_gu_uCookie: Cookie, returnUrl: ReturnUrl): Future[Either[Seq[ServiceError], Result]] = {
    val response = identityService.assignGroupCode(group.getCodeValue, sc_gu_uCookie)
    response.map{
      case Left(errors) => Left(errors)
      case Right(_) => Right(SeeOther(returnUrl.url))
    }
  }

  def checkUserForGroupMembership(group: GroupCode, cookie: Cookie): Future[Either[Seq[ServiceError], Boolean]] = {
    identityService.getUser(cookie).map{
      case Right(user) => {
        Right(isUserInGroup(user, group.getCodeValue))
      }
      case Left(errors) => {
        logger.error("Request did not have a SC_GU_U cookie could not get user.")
        Left(errors)
      }
    }
  }


  def isUserInGroup(user: User, group: String): Boolean = {
    val usersGroups = user.userGroups
    usersGroups.map(_.packageCode == group).contains(true)
  }

  private val addUserToGroupRequestFormBody = Form(
    mapping(
      "groupCode" -> text,
      "returnUrl" -> optional(text)
    )(AddUserToGroupRequest.apply)(AddUserToGroupRequest.unapply)
  )

}