package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_CHAT_LINK
import mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_UNKNOWN_LINK
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import timber.log.Timber

@Deprecated("Use GetChatLinkContentUseCase instead")
class LoadPreviewListener(context: Context?) : ChatBaseListener(context) {
    private var callback: OnPreviewLoadedCallback? = null
    private var callbackChatLink: OnChatPreviewLoadedCallback? = null
    private var type: Int = CHECK_LINK_TYPE_UNKNOWN_LINK

    constructor(
        context: Context?,
        callback: OnPreviewLoadedCallback,
        callbackChatLink: OnChatPreviewLoadedCallback,
        type: Int,
    ) : this(context) {
        this.callbackChatLink = callbackChatLink
        this.callback = callback
        this.type = type
    }

    constructor(
        context: Context?,
        callback: OnPreviewLoadedCallback,
        type: Int,
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
                callbackChatLink?.onPreviewLoaded(request, e.errorCode)
            }

            else -> {
                if (e.errorCode == MegaError.API_OK || e.errorCode == MegaError.API_EEXIST) {
                    Timber.d("Preview loaded")
                    callback?.onPreviewLoaded(request, e.errorCode == MegaError.API_EEXIST)
                } else {
                    Timber.e("Error loading preview. Error code ${e.errorCode}")
                    callback?.onErrorLoadingPreview(e.errorCode)
                }
            }
        }
    }

    interface OnPreviewLoadedCallback {
        fun onPreviewLoaded(request: MegaChatRequest, alreadyExist: Boolean)

        fun onErrorLoadingPreview(errorCode: Int)
    }

    interface OnChatPreviewLoadedCallback {
        fun onPreviewLoaded(request: MegaChatRequest, errorCode: Int)
    }
}