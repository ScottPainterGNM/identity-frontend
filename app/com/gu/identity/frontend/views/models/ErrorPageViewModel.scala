package com.gu.identity.frontend.views.models

import com.gu.identity.frontend.configuration.Configuration
import com.gu.identity.frontend.models.text.{NotFoundErrorPageText, ErrorPageText}
import play.api.i18n.Messages


case class ErrorPageViewModel private(
    layout: LayoutViewModel,

    text: ErrorPageText,

    resources: Seq[PageResource with Product],
    indirectResources: Seq[PageResource with Product])
  extends ViewModel with ViewModelResources


object ErrorPageViewModel {
  def apply(configuration: Configuration)(implicit messages: Messages): ErrorPageViewModel = {
    val layout = LayoutViewModel(configuration)

    ErrorPageViewModel(
      layout = layout,

      text = NotFoundErrorPageText(),

      resources = layout.resources,
      indirectResources = layout.indirectResources
    )
  }
}
