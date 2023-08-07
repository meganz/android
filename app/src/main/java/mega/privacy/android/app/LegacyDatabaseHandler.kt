package mega.privacy.android.app

import android.database.sqlite.SQLiteDatabase
import mega.privacy.android.app.main.megachat.ChatItemPreferences
import mega.privacy.android.app.objects.SDTransfer
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.chat.PendingMessage

/**
 * Legacy database handler
 *
 * it contains methods depending on app models, we will move to the data module later
 */
interface LegacyDatabaseHandler : DatabaseHandler {
    val offlineFiles: ArrayList<MegaOffline>

    val sdTransfers: ArrayList<SDTransfer>

    fun setChatItemPreferences(chatPrefs: ChatItemPreferences)
    fun findChatPreferencesByHandle(handle: String?): ChatItemPreferences?

    fun setOfflineFile(offline: MegaOffline): Long
    fun setOfflineFile(offline: MegaOffline, db: SQLiteDatabase): Long
    fun setOfflineFileOld(offline: MegaOffline): Long
    fun setOfflineFileOld(offline: MegaOffline, db: SQLiteDatabase): Long
    fun getOfflineFilesOld(db: SQLiteDatabase): ArrayList<MegaOffline>
    fun findByHandle(handle: Long): MegaOffline?
    fun findByHandle(handle: String?): MegaOffline?
    fun findByParentId(parentId: Int): ArrayList<MegaOffline>
    fun findById(id: Int): MegaOffline?
    fun findByPath(path: String?): ArrayList<MegaOffline>
    fun findbyPathAndName(path: String?, name: String?): MegaOffline?
    fun deleteOfflineFile(mOff: MegaOffline): Int


    /**
     * Adds a pending message from File Explorer.
     *
     * @param message Pending message to add.
     * @return The identifier of the pending message.
     */
    fun addPendingMessageFromFileExplorer(message: PendingMessage): Long

    /**
     * Adds a pending message.
     *
     * @param message Pending message to add.
     * @param state   State of the pending message.
     * @return The identifier of the pending message.
     */
    fun addPendingMessage(message: PendingMessage): Long

    /**
     * Adds a pending message.
     *
     * @param message Pending message to add.
     * @return The identifier of the pending message.
     */
    fun addPendingMessage(
        message: PendingMessage,
        state: Int,
    ): Long

    fun findPendingMessageById(messageId: Long): PendingMessage?
    fun addSDTransfer(transfer: SDTransfer): Long
    fun saveBackup(backup: Backup): Boolean
}
