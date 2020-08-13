package mega.privacy.android.app.fragments.offline

import mega.privacy.android.app.MegaOffline
import java.io.File

class OfflineNode(
    val node: MegaOffline,
    val thumbnail: File?,
    val nodeInfo: String,
    var selected: Boolean
) {
    companion object {
        val PLACE_HOLDER =
            OfflineNode(MegaOffline(-1, "-1", "", "", -1, "", -1, ""), null, "", false)
    }
}
