package com.gu.identity.frontend.controllers

import com.gu.identity.cookie.IdentityCookieDecoder
import com.gu.identity.frontend.authentication.AuthenticationService
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext}

class ThirdPartyTsAndCs(identityCookieDecoder: IdentityCookieDecoder) extends Controller{

  implicit lazy val executionContext: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val authenticationAction = new AuthenticatedBuilder(
    AuthenticationService.authenticatedUserFor(_, identityCookieDecoder.getUserDataForScGuU),
    _ => SeeOther("/signin")
  )

  def confirm = authenticationAction.async{ implicit request => {
      Future.successful(Ok("Some Ts and Cs"))
    }
  }
}