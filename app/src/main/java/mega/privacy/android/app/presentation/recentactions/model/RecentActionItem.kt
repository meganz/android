package mega.privacy.android.app.presentation.recentactions.model

import android.content.Context
import com.brandongogetap.stickyheaders.exposed.StickyHeader
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.MegaRecentActionBucket

/**
 * Hold the data for the the adapter recent action bucket item
 */
open class RecentActionItem {
    var viewType = 0
    var bucket: MegaRecentActionBucket? = null
    var date = ""
    var time = ""

    constructor(context: Context, _bucket: MegaRecentActionBucket) {
        viewType = TYPE_BUCKET
        bucket = _bucket
        date = TimeUtils.formatBucketDate(context, _bucket.timestamp)
        time = TimeUtils.formatTime(_bucket.timestamp)
    }

    constructor(_date: String) {
        viewType = TYPE_HEADER
        date = _date
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_BUCKET = 1
    }
}

/**
 * Hold the data for the adapter header item
 */
class RecentActionItemHeader(date: String) : RecentActionItem(date), StickyHeader