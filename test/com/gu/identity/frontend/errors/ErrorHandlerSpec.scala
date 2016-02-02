package com.gu.identity.frontend.errors

import com.gu.identity.frontend.configuration.Configuration
import org.scalatest.BeforeAndAfter
import play.api.{Configuration => PlayConfiguration}
import play.api.mvc.Result
import play.api._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play.PlaySpec
import play.twirl.api.Html

import scala.concurrent.Future


class ErrorHandlerSpec extends PlaySpec with BeforeAndAfter {

  val env = Environment.simple(mode = Mode.Prod)

  lazy val mockedErrorHandler = new MockedErrorHandler(testAppConfiguration)

  // crude state holder for error passed to error handler
  var lastError: Option[HttpError] = None

  before {
    lastError = None
  }

  "Error handler" must {

    "display 403 forbidden" in {
      val resp = mockedErrorHandler.onClientError(FakeRequest(), 403, "go away")

      status(resp) must equal(403)
      contentAsString(resp) must include("mocked error page")

      lastError.value mustBe a [ForbiddenError]
      lastError.value.asInstanceOf[ForbiddenError].message must equal("go away")
    }

    "display 404 not found error" in {
      val resp = mockedErrorHandler.onClientError(FakeRequest(), 404, "thing not found")

      status(resp) must equal(404)
      contentAsString(resp) must include("mocked error page")

      lastError.value mustBe a [NotFoundError]
      lastError.value.asInstanceOf[NotFoundError].message must equal("thing not found")
    }

    "display 400 bad request error" in {
      val resp = mockedErrorHandler.onClientError(FakeRequest(), 400, "bad thing")

      status(resp) must equal(400)
      contentAsString(resp) must include("mocked error page")

      lastError.value mustBe a [BadRequestError]
      lastError.value.asInstanceOf[BadRequestError].message must equal("bad thing")
    }

    "display 410 gone error" in {
      val resp = mockedErrorHandler.onClientError(FakeRequest(), 410, "goooooone")

      status(resp) must equal(410)
      contentAsString(resp) must include("mocked error page")

      lastError.value mustBe a [BadRequestError]
      lastError.value.asInstanceOf[BadRequestError].message must equal("goooooone")
    }

    "display 500 error" in {
      val err = new RuntimeException("failed!")
      val resp = mockedErrorHandler.onServerError(FakeRequest(), err)

      status(resp) must equal(500)
      contentAsString(resp) must include("mocked error page")

      lastError.value mustBe a [UnexpectedError]
      lastError.value.asInstanceOf[UnexpectedError].description must equal("RuntimeException: failed!")
    }

  }


  class MockedErrorHandler(
      configuration: Configuration)

    extends ErrorHandler(
      configuration,
      messagesApi = null,
      environment = Environment.simple(mode = Mode.Prod),
      sourceMapper = None,
      router = None) {

    override def renderErrorPage(error: HttpError, resultGenerator: Html => Result) = {
      lastError = Some(error)

      Future.successful(resultGenerator(Html("mocked error page")))
    }
  }


  lazy val testAppConfiguration = new Configuration {
    override val identityApiHost: String = "identityApiHost"
    override val identityApiKey: String = "identityApiKey"
    override val identityCookieDomain: String = "theguardian.com"
    override val identityProfileBaseUrl: String = "profile.theguardian.com"
    override val omnitureAccount: String = "omnitureAccount"
    override val appConfiguration = PlayConfiguration.empty
    override val identityFederationApiHost: String = "https://oauth.theguardian.com"
  }

}
