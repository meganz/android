package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.*

class LoadPreviewListener(context: Context?) : ChatBaseListener(context) {
    private var callback: OnPreviewLoadedCallback? = null
    private var callbackChatLink: OnChatPreviewLoadedCallback? = null
    private var type: Int = CHECK_LINK_TYPE_UNKNOWN_LINK

    constructor(
        context: Context?,
        callback: OnPreviewLoadedCallback,
        callbackChatLink: OnChatPreviewLoadedCallback,
        type: Int
    ) : this(context) {
        this.callbackChatLink = callbackChatLink
        this.callback = callback
        this.type = type
    }

    constructor(
        context: Context?,
        callback: OnPreviewLoadedCallback,
        type: Int
    ) : this(context) {
        this.callback = callback
        this.callbackChatLink = null
        this.type = type
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_LOAD_PREVIEW) {
            return
        }

        when (type) {
            CHECK_LINK_TYPE_CHAT_LINK -> {
                callbackChatLink?.onPreviewLoaded(
                    api,
                    request.chatHandle,
                    e.errorCode,
                    request.userHandle,
                    request.paramType
                )
            }
            else -> {
                if (e.errorCode == MegaError.API_OK || e.errorCode == MegaError.API_EEXIST) {
                    logDebug("Preview loaded")
                    callback?.onPreviewLoaded(
                        api,
                        request.chatHandle,
                        request.text,
                        request.megaHandleList,
                        request.paramType,
                        request.link,
                        request.flag
                    )
                } else {
                    LogUtil.logError("Error loading preview. Error code " + e.errorCode)
                    callback?.onErrorLoadingPreview(e.errorCode)
                }
            }
        }
    }

    interface OnPreviewLoadedCallback {
        fun onPreviewLoaded(
            api: MegaChatApiJava,
            chatId: Long,
            titleChat: String?,
            handleList: MegaHandleList?,
            paramType: Int,
            link: String?,
            isFromOpenChatPreview: Boolean
        )

        fun onErrorLoadingPreview(errorCode: Int)
    }

    interface OnChatPreviewLoadedCallback {
        fun onPreviewLoaded(
            api: MegaChatApiJava,
            chatId: Long,
            error: Int,
            userHandle: Long,
            paramType: Int
        )
    }
}