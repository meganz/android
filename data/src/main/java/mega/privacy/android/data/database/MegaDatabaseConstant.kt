package mega.privacy.android.data.database

/**
 * Database constant
 * Share constant between legacy SQLite and new Room database
 */
object MegaDatabaseConstant {
    /**
     * Database Version
     */
    const val DATABASE_VERSION = 79

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
    const val TABLE_COMPLETED_TRANSFERS = "completedtransfers"

    /**
     * Table Active Transfers
     */
    const val TABLE_ACTIVE_TRANSFERS = "active_transfers"

    /**
     * Table Sync Records
     */
    const val TABLE_SYNC_RECORDS = "syncrecords"

    /**
     * Table Sd Transfers
     */
    const val TABLE_SD_TRANSFERS = "sdtransfers"

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
}
