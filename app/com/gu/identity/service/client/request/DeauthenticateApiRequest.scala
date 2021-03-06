package com.gu.identity.service.client.request

import com.gu.identity.frontend.models.TrackingData
import com.gu.identity.service.client._
import play.api.mvc.Cookie

case class DeauthenticateApiRequest private(
    val url: String,
    private val extraHeaders: HttpParameters = Nil,
    private val extraParameters: HttpParameters = Nil)
  extends ApiRequest {
    override val method = POST
    override val headers = Seq("Content-Type" -> "application/json") ++ extraHeaders
    override val parameters = extraParameters
}

object DeauthenticateApiRequest {

  def apply(secureCookie: Cookie, trackingData: TrackingData)(implicit configuration: IdentityClientConfiguration): DeauthenticateApiRequest = {
    lazy val extraHeaders = ApiRequest.commonApiHeaders(trackingData) ++ ApiRequest.apiSecureCookieUserHeader(secureCookie)
    lazy val extraParameters = trackingData.parameters
    DeauthenticateApiRequest(ApiRequest.apiEndpoint("unauth"), extraHeaders = extraHeaders, extraParameters = extraParameters)
  }

}
