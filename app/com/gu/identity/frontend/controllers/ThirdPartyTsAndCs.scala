package com.gu.identity.frontend.controllers

import com.gu.identity.cookie.IdentityCookieDecoder
import com.gu.identity.frontend.authentication.{CookieName, AuthenticationService}
import com.gu.identity.frontend.configuration.Configuration
import com.gu.identity.frontend.models.{GroupCode, ClientID, ReturnUrl}
import com.gu.identity.frontend.services.{ServiceError, IdentityService}
import com.gu.identity.service.client.models.User
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._
import com.gu.identity.frontend.logging.Logging
import com.gu.identity.frontend.views.ViewRenderer.renderTsAndCs

import scala.concurrent.{Future, ExecutionContext}

class ThirdPartyTsAndCs(identityService: IdentityService, identityCookieDecoder: IdentityCookieDecoder, config: Configuration, val messagesApi: MessagesApi) extends Controller with Logging with I18nSupport {

  implicit lazy val executionContext: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val authenticationAction = new AuthenticatedBuilder(
    AuthenticationService.authenticatedUserFor(_, identityCookieDecoder.getUserDataForScGuU),
    _ => SeeOther("/signin")
  )

  def confirm(group: String, returnUrl: Option[String], clientId: Option[String]) = authenticationAction.async{ implicit request => {
      val clientIdActual = ClientID(clientId)
      val groupCode = GroupCode(group)
      val sc_gu_uCookie = getSC_GU_UCookie(request.cookies)
      val verifiedReturnUrl = ReturnUrl(returnUrl, request.headers.get("Referer"), config)

      (groupCode, sc_gu_uCookie) match {
        case(Some(validGroup), Some(cookie)) => {
          checkUserForGroupMembership(group, cookie).map{
            case Right(true) => SeeOther(verifiedReturnUrl.url)
            case Right(false) => renderTsAndCs(config, clientIdActual, validGroup, verifiedReturnUrl)
            case Left(errors) => {
              logger.warn(s"Could not check user's group membership status {}", errors)
              BadRequest
            }
          }
        }
        case(None, _) => {
          logger.info(s"Received invalid group code $group")
          Future(BadRequest)
        }
        case(_, None) => {
          logger.info("Request did not have a SC_GU_U cookie")
          Future(BadRequest)
        }
      }
    }
  }

  def checkUserForGroupMembership(group: String, cookie: Cookie): Future[Either[Seq[ServiceError], Boolean]] = {
    identityService.getUser(cookie).map{
      case Right(user) => {
        Right(isUserInGroup(user, group))
      }
      case Left(errors) => {
        logger.info("Request did not have a SC_GU_U cookie could not get user.")
        Left(errors)
      }
    }
  }


  def addToGroup() = authenticationAction.async { implicit request => {
//    val sc_gu_uCookie = getSC_GU_UCookie(request.cookies)
//    sc_gu_uCookie match {
//      case Some(cookie) => {
//        val response = identityService.assignGroupCode(group, cookie)
//        Future.successful(Ok("ddf"))
//      }
//        case _ => Future(NotFound)
//      }
    Future(Ok("Successful post"))
    }

  }

//  def assignToGroup(group: String, cookie: Cookie, returnUrl: ReturnUrl) = {
//    checkUserForGroupMembership(group, cookie).map {
//      case Right(true) => SeeOther(returnUrl.url)
//      case Right(false) => SeeOther(routes.ThirdPartyTsAndCs.addToGroup(group)).withCookies(cookie)
//      case _ => {
//        logger.info("could not check users group membership status")
//        BadRequest
//      }
//    }
//  }

  def isUserInGroup(user: User, group: String):Boolean = {
    val usersGroups = user.userGroups
    usersGroups.map(_.packageCode == group).contains(true)
  }

  def getSC_GU_UCookie(cookies: Cookies): Option[Cookie] = cookies.get(CookieName.SC_GU_U.toString)
}
