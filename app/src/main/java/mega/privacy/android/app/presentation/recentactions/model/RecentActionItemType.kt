package mega.privacy.android.app.presentation.recentactions.model

import com.brandongogetap.stickyheaders.exposed.StickyHeader
import nz.mega.sdk.MegaRecentActionBucket

/**
 *  Define the different recent action item type hold by [RecentsAdapter]
 *
 *  @property timestamp the timestamp of the item
 */
sealed class RecentActionItemType(val timestamp: Long) {

    /**
     *  Define a generic item hold by [Recentsadapter]
     *
     *  @property bucket a [MegaRecentActionBucket]
     *  @property userName the name of the user associated to the bucket
     */
    class Item(val bucket: MegaRecentActionBucket, val userName: String = "") :
        RecentActionItemType(timestamp = bucket.timestamp)

    /**
     * Define a header hold by [RecentsAdapter]
     */
    class Header(_timestamp: Long) :
        RecentActionItemType(timestamp = _timestamp), StickyHeader
}