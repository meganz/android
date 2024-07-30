package mega.privacy.android.app

import mega.privacy.android.data.database.DatabaseHandler

/**
 * Legacy database handler
 *
 * it contains methods depending on app models, we will move to the data module later
 */
interface LegacyDatabaseHandler : DatabaseHandler {
    fun findByHandle(handle: Long): MegaOffline?
    fun findByHandle(handle: String?): MegaOffline?
    fun findByParentId(parentId: Int): ArrayList<MegaOffline>
    fun findById(id: Int): MegaOffline?
}
