package mega.privacy.android.app.fragments.offline

import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.utils.Constants.INVALID_ID
import java.io.File

class OfflineNode(
    val node: MegaOffline,
    val thumbnail: File?,
    val nodeInfo: String,
    var selected: Boolean,
    var uiDirty: Boolean = false
) {
    companion object {
        val PLACE_HOLDER =
            OfflineNode(
                MegaOffline(INVALID_ID, "-1", "", "", INVALID_ID, "", INVALID_ID, ""),
                null, "", false
            )
        val HEADER = OfflineNode(
            MegaOffline(INVALID_ID, "-2", "", "", INVALID_ID, "", INVALID_ID, ""),
            null, "", false
        )
    }
}
