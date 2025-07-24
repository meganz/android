package mega.privacy.android.data.database

/**
 * Database constant
 * Share constant between legacy SQLite and new Room database
 */
object MegaDatabaseConstant {
    /**
     * Database Version
     */
    const val DATABASE_VERSION = 112

    /**
     * Database Name
     */
    const val DATABASE_NAME = "megapreferences"

    /**
     * Table Contacts
     */
    const val TABLE_CONTACTS = "contacts"

    /**
     * Table Completed Transfers
     */
    const val TABLE_COMPLETED_TRANSFERS = "completedtransfers_2"

    /**
     * Table Completed Transfers legacy
     */
    const val TABLE_COMPLETED_TRANSFERS_LEGACY = "completedtransfers"

    /**
     * Table Active Transfers
     */
    const val TABLE_ACTIVE_TRANSFERS = "active_transfers"

    /**
     * Table Active Transfer Groups
     */
    const val TABLE_ACTIVE_TRANSFER_ACTION_GROUPS = "active_transfer_groups"

    /**
     * Table Backups
     */
    const val TABLE_BACKUPS = "backups"

    /**
     * Table Camera Uploads Records
     */
    const val TABLE_CAMERA_UPLOADS_RECORDS = "camerauploadsrecords"

    /**
     * Table Offline
     */
    const val TABLE_OFFLINE = "offline"

    /**
     * Table For Android Sync solved issues list
     */
    const val TABLE_SYNC_SOLVED_ISSUES = "syncsolvedissues"

    /**
     * Table For Android Sync solved issues list
     */
    const val TABLE_SYNC_SHOWN_NOTIFICATIONS = "syncshownnotifications"

    /**
     * Table For Android Sync paused syncs list
     */
    const val TABLE_USER_PAUSED_SYNCS = "userpausedsyncs"

    /**
     * Table Chat Room Perference
     */
    const val TABLE_CHAT_ROOM_PREFERENCE = "chatroompreference"

    /**
     * Passphrase File Name
     */
    const val PASSPHRASE_FILE_NAME = "passphrase.bin"

    /**
     * Table recently watched video
     */
    const val TABLE_RECENTLY_WATCHED_VIDEO = "recentlywatchedvideo"

    /**
     * Table pending transfers
     */
    const val TABLE_PENDING_TRANSFER = "pending_transfer"

    /**
     * Table for last page viewed in PDF viewer
     */
    const val TABLE_LAST_PAGE_VIEWED_IN_PDF = "last_page_viewed_in_pdf"

    /**
     * Table for media playback info
     */
    const val TABLE_MEDIA_PLAYBACK_INFO = "media_playback_info"
}
