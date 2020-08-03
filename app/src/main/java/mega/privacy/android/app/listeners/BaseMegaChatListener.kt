package mega.privacy.android.app.listeners

import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatPresenceConfig

interface BaseMegaChatListener : MegaChatListenerInterface {
  override fun onChatListItemUpdate(
    api: MegaChatApiJava,
    item: MegaChatListItem?
  ) {
  }

  override fun onChatInitStateUpdate(
    api: MegaChatApiJava,
    newState: Int
  ) {
  }

  override fun onChatOnlineStatusUpdate(
    api: MegaChatApiJava,
    userhandle: Long,
    status: Int,
    inProgress: Boolean
  ) {
  }

  override fun onChatPresenceConfigUpdate(
    api: MegaChatApiJava,
    config: MegaChatPresenceConfig?
  ) {
  }

  override fun onChatConnectionStateUpdate(
    api: MegaChatApiJava,
    chatid: Long,
    newState: Int
  ) {
  }

  override fun onChatPresenceLastGreen(
    api: MegaChatApiJava,
    userhandle: Long,
    lastGreen: Int
  ) {
  }
}
