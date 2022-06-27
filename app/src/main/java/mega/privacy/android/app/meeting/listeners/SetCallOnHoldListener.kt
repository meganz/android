package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import timber.log.Timber

class SetCallOnHoldListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnCallOnHoldCallback? = null
    private var snackbarShower: SnackbarShower? = null

    constructor(
        context: Context?,
        snackbarShower: SnackbarShower,
    ) : this(context) {
        this.snackbarShower = snackbarShower
    }

    constructor(
        context: Context?,
        callback: OnCallOnHoldCallback,
    ) : this(context) {
        this.callback = callback
    }

    constructor(
        context: Context?,
        snackbarShower: SnackbarShower,
        callback: OnCallOnHoldCallback,
    ) : this(context) {
        this.callback = callback
        this.snackbarShower = snackbarShower
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_SET_CALL_ON_HOLD) {
            return
        }

        when (e.errorCode) {
            MegaError.API_OK -> {
                Timber.d("Call on hold")
                callback?.onCallOnHold(request.chatHandle, request.flag)
            }
            MegaChatError.ERROR_NOENT -> {
                Timber.w("Error. No calls in this chat ${e.errorString}, error code ${e.errorCode}")
            }
            MegaChatError.ERROR_ACCESS -> {
                Timber.w("Error. The call is not in progress ${e.errorString}, error code ${e.errorCode}")
                snackbarShower?.showSnackbar(StringResourcesUtils.getString(R.string.call_error_call_on_hold))
            }
            MegaChatError.ERROR_ARGS -> {
                Timber.w("Error. The call was already in that state ${e.errorString}, error code ${e.errorCode}")
            }
        }
    }

    interface OnCallOnHoldCallback {
        fun onCallOnHold(chatId: Long, isOnHold: Boolean)
    }
}