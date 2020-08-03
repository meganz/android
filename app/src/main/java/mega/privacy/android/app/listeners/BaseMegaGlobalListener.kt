package mega.privacy.android.app.listeners

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaEvent
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserAlert
import java.util.ArrayList

interface BaseMegaGlobalListener : MegaGlobalListenerInterface {
  override fun onUsersUpdate(
    api: MegaApiJava,
    users: ArrayList<MegaUser>?
  ) {
  }

  override fun onUserAlertsUpdate(
    api: MegaApiJava,
    userAlerts: ArrayList<MegaUserAlert>?
  ) {
  }

  override fun onNodesUpdate(
    api: MegaApiJava,
    nodeList: ArrayList<MegaNode>?
  ) {
  }

  override fun onReloadNeeded(api: MegaApiJava) {}
  override fun onAccountUpdate(api: MegaApiJava) {}
  override fun onContactRequestsUpdate(
    api: MegaApiJava,
    requests: ArrayList<MegaContactRequest>?
  ) {
  }

  override fun onEvent(
    api: MegaApiJava,
    event: MegaEvent
  ) {
  }
}
