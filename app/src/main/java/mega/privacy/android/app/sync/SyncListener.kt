package mega.privacy.android.app.sync

import android.content.Context
import mega.privacy.android.app.sync.mock.MockListener
import mega.privacy.android.app.sync.mock.RequestType
import mega.privacy.android.app.sync.mock.SyncEventResult
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaError


class SyncListener(context: Context) : MockListener {

    var callback: SyncEventCallback? = null

    override fun onFinish(result: SyncEventResult, errorCode: Int) {
        val requestType = when (result.requestType) {
            RequestType.REQUEST_TYPE_UPDATE -> "UPDATE"
            RequestType.REQUEST_TYPE_SET -> "SET"
            RequestType.REQUEST_TYPE_DELETE -> "DELETE"
        }

        if (result.requestType.value == callback?.requestType()) {
            if (errorCode == MegaError.API_OK) {
                logDebug("Request $requestType successfully.")
                callback?.onSuccess(result, null, null, null)
            } else {
                logDebug("Request $requestType failed.")
                callback?.onFail(result, null)
            }
        }
    }
}