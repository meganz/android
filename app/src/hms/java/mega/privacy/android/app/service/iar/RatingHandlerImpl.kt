package mega.privacy.android.app.service.iar

import android.content.Context
import mega.privacy.android.app.middlelayer.iar.OnCompleteListener
import mega.privacy.android.app.middlelayer.iar.RatingHandler
import mega.privacy.android.app.utils.LogUtil
/**
 * Implement rating feature for HMS by using huawei APIs
 *
 * @param context Context for getting rating dialog
 */
class RatingHandlerImpl(context: Context) : RatingHandler(context) {

    override fun showReviewDialog(context: Context, listener: OnCompleteListener) {
        LogUtil.logDebug("showHmsReviewDialog")
    }
}