package mega.privacy.android.app.repo

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.utils.SortUtil.sortOfflineByModificationDateAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByModificationDateDescending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByNameAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByNameDescending
import mega.privacy.android.app.utils.SortUtil.sortOfflineBySizeAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineBySizeDescending
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_ASC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_DESC
import javax.inject.Inject

class MegaNodeRepo @Inject constructor(
    private val dbHandler: DatabaseHandler
) {
    fun loadOfflineNodes(path: String, order: Int): List<MegaOffline> {
        val nodes = dbHandler.findByPath(path)
        when (order) {
            ORDER_DEFAULT_DESC -> {
                sortOfflineByNameDescending(nodes)
            }
            ORDER_DEFAULT_ASC -> {
                sortOfflineByNameAscending(nodes)
            }
            ORDER_MODIFICATION_ASC -> {
                sortOfflineByModificationDateAscending(nodes)
            }
            ORDER_MODIFICATION_DESC -> {
                sortOfflineByModificationDateDescending(nodes)
            }
            ORDER_SIZE_ASC -> {
                sortOfflineBySizeAscending(nodes)
            }
            ORDER_SIZE_DESC -> {
                sortOfflineBySizeDescending(nodes)
            }
            else -> {
            }
        }
        return nodes
    }
}
