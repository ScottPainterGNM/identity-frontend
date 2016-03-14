package com.gu.identity.frontend.request

import com.gu.identity.frontend.configuration.Configuration
import com.gu.identity.frontend.errors._
import com.gu.identity.frontend.models.{ReturnUrl, ClientID, GroupCode}
import com.gu.identity.frontend.request.RequestParameters._
import play.api.data.Forms._
import play.api.data.{FormError, Mapping, Form}
import play.api.http.HeaderNames
import play.api.mvc.{Result, BodyParsers, BodyParser}


case class RegisterActionRequestBody(
    firstName: String,
    lastName: String,
    email: String,
    username: String,
    password: String,
    receiveGnmMarketing: Boolean,
    receive3rdPartyMarketing: Boolean,
    returnUrl: ReturnUrl,
    skipConfirmation: Option[Boolean],
    groupCode: Option[GroupCode],
    clientId: Option[ClientID],
    csrfToken: String)
  extends ReturnUrlRequestParameter
  with SkipConfirmationRequestParameter
  with ClientIdRequestParameter
  with GroupRequestParameter
  with CSRFTokenRequestParameter

object RegisterActionRequestBody {

  def bodyParser(configuration: Configuration) =
    BodyParser("RegisterActionRequestBody") { requestHeader =>
      val refererHeader = requestHeader.headers.get(HeaderNames.REFERER)
      val form = Form {
        FormMapping.registerForm(configuration, refererHeader)
      }

      BodyParsers.parse.form(form, onErrors = onParserErrors).apply(requestHeader)
    }

  // Unfortunately need to throw errors here as play's parser syntax doesn't
  // allow returning a typed error, only a result
  private def onParserErrors(form: Form[RegisterActionRequestBody]): Result = throw {
    if (form.errors.size == 1) formErrorToAppException(form.errors.head)
    else SeqAppExceptions {
      form.errors.map(formErrorToAppException)
    }
  }

  private def formErrorToAppException(formError: FormError): AppException = formError match {
    case FormError("csrfToken", _, _) => ForgeryTokenAppException("Missing csrfToken on request")
    case FormError("username", msg, _) => RegisterActionInvalidUsernameAppException(msg.headOption.getOrElse("unknown"))
    case FormError("password", msg, _) => RegisterActionInvalidPasswordAppException(msg.headOption.getOrElse("unknown"))
    case FormError("groupCode", msg, _) => RegisterActionInvalidGroupAppException(msg.headOption.getOrElse("unknown"))
    case e => RegisterActionBadRequestAppException(s"Unexpected error: ${e.message}")
  }


  object FormMapping {
    import ClientID.FormMapping.clientId
    import GroupCode.FormMappings.groupCode
    import ReturnUrl.FormMapping.returnUrl

    private val username: Mapping[String] = text.verifying(
      "error.username", name => name.matches("[A-z0-9]+") && name.length > 5 && name.length < 21
    )

    private val password: Mapping[String] = text.verifying(
      "error.password", name => name.length > 5 && name.length < 21
    )

    def registerForm(configuration: Configuration, refererHeader: Option[String]): Mapping[RegisterActionRequestBody] =
      mapping(
        "firstName" -> nonEmptyText,
        "lastName" -> nonEmptyText,
        "email" -> email,
        "username" -> username,
        "password" -> password,
        "receiveGnmMarketing" -> boolean,
        "receive3rdPartyMarketing" -> boolean,
        "returnUrl" -> returnUrl(refererHeader, configuration),
        "skipConfirmation" -> optional(boolean),
        "groupCode" -> optional(groupCode),
        "clientId" -> optional(clientId),
        "csrfToken" -> text
      )(RegisterActionRequestBody.apply)(RegisterActionRequestBody.unapply)
  }
}
