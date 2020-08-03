package mega.privacy.android.app.listeners

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

interface BaseMegaRequestListener : MegaRequestListenerInterface {
  override fun onRequestStart(
    api: MegaApiJava,
    request: MegaRequest
  ) {
  }

  override fun onRequestUpdate(
    api: MegaApiJava,
    request: MegaRequest
  ) {
  }

  override fun onRequestFinish(
    api: MegaApiJava,
    request: MegaRequest,
    e: MegaError
  ) {
  }

  override fun onRequestTemporaryError(
    api: MegaApiJava,
    request: MegaRequest,
    e: MegaError
  ) {
  }
}
