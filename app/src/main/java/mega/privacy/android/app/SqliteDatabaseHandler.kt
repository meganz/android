package mega.privacy.android.app

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.provider.Settings
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.database.LegacyDatabaseMigration
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_OFFLINE
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_SD_TRANSFERS
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.mapper.StorageStateIntMapper
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_ON
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject


/**
 * Sqlite implementation of database handler
 */
class SqliteDatabaseHandler @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val storageStateMapper: StorageStateMapper,
    private val storageStateIntMapper: StorageStateIntMapper,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val sqLiteOpenHelper: Lazy<SupportSQLiteOpenHelper>,
    private val legacyDatabaseMigration: LegacyDatabaseMigration,
) : LegacyDatabaseHandler {
    private val writableDatabase: SupportSQLiteDatabase by lazy { sqLiteOpenHelper.get().writableDatabase }
    private val readableDatabase: SupportSQLiteDatabase by lazy { sqLiteOpenHelper.get().readableDatabase }

    //get the credential of last login
    override val credentials: UserCredentials?
        get() {
            var userCredentials: UserCredentials? = null
            val selectQuery = "SELECT * FROM $TABLE_CREDENTIALS"
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    //get the credential of last login
                    if (cursor.moveToFirst()) {
                        val email = decrypt(cursor.getString(1))
                        val session = decrypt(cursor.getString(2))
                        val firstName = decrypt(cursor.getString(3))
                        val lastName = decrypt(cursor.getString(4))
                        val myHandle = decrypt(cursor.getString(5))
                        userCredentials =
                            UserCredentials(email, session, firstName, lastName, myHandle)
                    }
                }
            } catch (e: SQLiteException) {
                legacyDatabaseMigration.onCreate(writableDatabase)
            } catch (e: Exception) {
                Timber.e(e, "Error decrypting DB field")
            }
            return userCredentials
        }

    override val ephemeral: EphemeralCredentials?
        get() {
            var ephemeralCredentials: EphemeralCredentials? = null
            val selectQuery = "SELECT * FROM $TABLE_EPHEMERAL"
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val email = decrypt(cursor.getString(1))
                        val password = decrypt(cursor.getString(2))
                        val session = decrypt(cursor.getString(3))
                        val firstName = decrypt(cursor.getString(4))
                        val lastName = decrypt(cursor.getString(5))
                        ephemeralCredentials =
                            EphemeralCredentials(email, password, session, firstName, lastName)
                    }
                }
            } catch (e: SQLiteException) {
                legacyDatabaseMigration.onCreate(writableDatabase)
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return ephemeralCredentials
        }

    /**
     * Gets preferences.
     *
     * @return Preferences.
     */
    override val preferences: MegaPreferences?
        get() = getPreferences(writableDatabase)

    /**
     * Gets preferences.
     *
     * @param db Current DB.
     * @return Preferences.
     */
    private fun getPreferences(db: SupportSQLiteDatabase): MegaPreferences? {
        var prefs: MegaPreferences? = null
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        try {
            db.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val firstTime =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_FIRST_LOGIN)))
                    val camSyncEnabled =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_ENABLED)))
                    val camSyncHandle =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_HANDLE)))
                    val camSyncLocalPath =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_LOCAL_PATH)))
                    val wifi = decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_WIFI)))
                    val fileUpload =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_FILE_UPLOAD)))
                    val pinLockEnabled =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_PASSCODE_LOCK_ENABLED)))
                    val pinLockCode =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_PASSCODE_LOCK_CODE)))
                    val askAlways =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_STORAGE_ASK_ALWAYS)))
                    val downloadLocation = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_STORAGE_DOWNLOAD_LOCATION
                            )
                        )
                    )
                    val camSyncTimeStamp =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_TIMESTAMP)))
                    val lastFolderUpload =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_UPLOAD_FOLDER)))
                    val lastFolderCloud = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_LAST_CLOUD_FOLDER_HANDLE
                            )
                        )
                    )
                    val secondaryFolderEnabled =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_FOLDER_ENABLED)))
                    val secondaryPath =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_FOLDER_LOCAL_PATH)))
                    val secondaryHandle =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_FOLDER_HANDLE)))
                    val secSyncTimeStamp =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SEC_SYNC_TIMESTAMP)))
                    val keepFileNames =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_KEEP_FILE_NAMES)))
                    val storageAdvancedDevices = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_STORAGE_ADVANCED_DEVICES
                            )
                        )
                    )
                    val preferredViewList =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_VIEW_LIST)))
                    val preferredViewListCamera = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_PREFERRED_VIEW_LIST_CAMERA
                            )
                        )
                    )
                    val uriExternalSDCard =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_URI_EXTERNAL_SD_CARD)))
                    val cameraFolderExternalSDCard = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD
                            )
                        )
                    )
                    val pinLockType =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_PASSCODE_LOCK_TYPE)))
                    val preferredSortCloud =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_SORT_CLOUD)))
                    val preferredSortOthers =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_SORT_OTHERS)))
                    val firstTimeChat =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_FIRST_LOGIN_CHAT)))
                    val isAutoPlayEnabled =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_AUTO_PLAY)))
                    val uploadVideoQuality =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_UPLOAD_VIDEO_QUALITY)))
                    val conversionOnCharging = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_CONVERSION_ON_CHARGING
                            )
                        )
                    )
                    val chargingOnSize =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_CHARGING_ON_SIZE)))
                    val camVideoSyncTimeStamp = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_CAM_VIDEO_SYNC_TIMESTAMP
                            )
                        )
                    )
                    val secVideoSyncTimeStamp = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_SEC_VIDEO_SYNC_TIMESTAMP
                            )
                        )
                    )
                    val removeGPS =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_REMOVE_GPS)))
                    val closeInviteBanner =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_INVITE_BANNER)))
                    val preferredSortCameraUpload = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_PREFERRED_SORT_CAMERA_UPLOAD
                            )
                        )
                    )
                    val sdCardUri =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SD_CARD_URI)))
                    val askForDisplayOver =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_ASK_FOR_DISPLAY_OVER)))
                    val askForSetDownloadLocation = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_ASK_SET_DOWNLOAD_LOCATION
                            )
                        )
                    )
                    val mediaSDCardUri = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_URI_MEDIA_EXTERNAL_SD_CARD
                            )
                        )
                    )
                    val isMediaOnSDCard = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD
                            )
                        )
                    )
                    val passcodeLockRequireTime = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_PASSCODE_LOCK_REQUIRE_TIME
                            )
                        )
                    )
                    val fingerprintLock =
                        if (cursor.getColumnIndex(KEY_FINGERPRINT_LOCK) != Constants.INVALID_VALUE) decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    KEY_FINGERPRINT_LOCK
                                )
                            )
                        ) else "false"
                    prefs = MegaPreferences(
                        firstTime,
                        wifi,
                        camSyncEnabled,
                        camSyncHandle,
                        camSyncLocalPath,
                        fileUpload,
                        camSyncTimeStamp,
                        pinLockEnabled,
                        pinLockCode,
                        askAlways,
                        downloadLocation,
                        lastFolderUpload,
                        lastFolderCloud,
                        secondaryFolderEnabled,
                        secondaryPath,
                        secondaryHandle,
                        secSyncTimeStamp,
                        keepFileNames,
                        storageAdvancedDevices,
                        preferredViewList,
                        preferredViewListCamera,
                        uriExternalSDCard,
                        cameraFolderExternalSDCard,
                        pinLockType,
                        preferredSortCloud,
                        preferredSortOthers,
                        firstTimeChat,
                        uploadVideoQuality,
                        conversionOnCharging,
                        chargingOnSize,
                        camVideoSyncTimeStamp,
                        secVideoSyncTimeStamp,
                        isAutoPlayEnabled,
                        removeGPS,
                        closeInviteBanner,
                        preferredSortCameraUpload,
                        sdCardUri,
                        askForDisplayOver,
                        askForSetDownloadLocation,
                        mediaSDCardUri,
                        isMediaOnSDCard,
                        passcodeLockRequireTime,
                        fingerprintLock
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return prefs
    }

    /**
     * Get chat settings from the current DB.
     *
     * @return Chat settings.
     */
    /**
     * Save chat settings in the current DB.
     *
     * @param chatSettings Chat settings to save.
     */
    override var chatSettings: ChatSettings?
        get() = getChatSettings(writableDatabase)
        set(chatSettings) {
            setChatSettings(writableDatabase, chatSettings)
        }

    /**
     * Get chat settings from the current DB.
     *
     * @param db Current DB.
     * @return Chat settings.
     */
    private fun getChatSettings(db: SupportSQLiteDatabase): ChatSettings? {
        Timber.d("getChatSettings")
        var chatSettings: ChatSettings? = null
        val selectQuery = "SELECT * FROM $TABLE_CHAT_SETTINGS"
        try {
            db.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val notificationSound = decrypt(cursor.getString(2))
                    val vibrationEnabled = decrypt(cursor.getString(3))
                    val videoQuality = decrypt(cursor.getString(4))
                    chatSettings =
                        ChatSettings(
                            notificationSound.orEmpty(),
                            vibrationEnabled ?: VIBRATION_ON,
                            videoQuality ?: VideoQuality.MEDIUM.value.toString()
                        )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return chatSettings
    }

    /**
     * Save chat settings in the DB.
     *
     * @param db           DB object to save the settings.
     * @param chatSettings Chat settings to save.
     */
    private fun setChatSettings(db: SupportSQLiteDatabase, chatSettings: ChatSettings?) {
        if (chatSettings == null) {
            Timber.e("Error: Chat settings are null")
            return
        }
        db.execSQL("DELETE FROM $TABLE_CHAT_SETTINGS")
        val values = ContentValues().apply {
            put(KEY_CHAT_NOTIFICATIONS_ENABLED, "")
            put(KEY_CHAT_SOUND_NOTIFICATIONS, encrypt(chatSettings.notificationsSound))
            put(KEY_CHAT_VIBRATION_ENABLED, encrypt(chatSettings.vibrationEnabled))
            put(KEY_CHAT_VIDEO_QUALITY, encrypt(chatSettings.videoQuality))
        }

        db.insert(TABLE_CHAT_SETTINGS, SQLiteDatabase.CONFLICT_NONE, values)
    }
    /**
     * Gets the chat video quality value.
     *
     * @return The chat video quality.
     */
    /**
     * Sets the chat video quality value.
     * There are four possible values for this setting: VIDEO_QUALITY_ORIGINAL, VIDEO_QUALITY_HIGH,
     * VIDEO_QUALITY_MEDIUM or VIDEO_QUALITY_LOW.
     *
     * @param chatVideoQuality The new chat video quality.
     */
    override var chatVideoQuality: Int
        get() {
            Timber.d("getChatVideoQuality")
            return getIntValue(
                TABLE_CHAT_SETTINGS,
                KEY_CHAT_VIDEO_QUALITY,
                VideoQuality.MEDIUM.value
            )
        }
        set(chatVideoQuality) {
            Timber.d("setChatVideoQuality")
            setIntValue(TABLE_CHAT_SETTINGS, KEY_CHAT_VIDEO_QUALITY, chatVideoQuality)
        }

    override fun setNotificationSoundChat(sound: String?) {
        val selectQuery = "SELECT * FROM $TABLE_CHAT_SETTINGS"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_CHAT_SETTINGS SET $KEY_CHAT_SOUND_NOTIFICATIONS= '${
                            encrypt(sound)
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CHAT_SOUND_NOTIFICATIONS, encrypt(sound))
                    writableDatabase.insert(
                        TABLE_CHAT_SETTINGS,
                        SQLiteDatabase.CONFLICT_NONE,
                        values
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setVibrationEnabledChat(enabled: String?) {
        val selectQuery = "SELECT * FROM $TABLE_CHAT_SETTINGS"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_CHAT_SETTINGS SET $KEY_CHAT_VIBRATION_ENABLED= '${
                            encrypt(enabled)
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CHAT_VIBRATION_ENABLED, encrypt(enabled))
                    writableDatabase.insert(
                        TABLE_CHAT_SETTINGS,
                        SQLiteDatabase.CONFLICT_NONE,
                        values
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    /**
     * Saves attributes in DB.
     *
     * @param db   DB object to save the attributes.
     * @param attr Attributes to save.
     */
    private fun setAttributes(db: SupportSQLiteDatabase, attr: MegaAttributes?) {
        if (attr == null) {
            Timber.e("Error: Attributes are null")
            return
        }
        val values = ContentValues()
        values.put(KEY_ATTR_ONLINE, encrypt(attr.online))
        values.put(KEY_ATTR_INTENTS, encrypt(Integer.toString(attr.attempts)))
        values.put(KEY_ATTR_ASK_SIZE_DOWNLOAD, encrypt(attr.askSizeDownload))
        values.put(KEY_ATTR_ASK_NOAPP_DOWNLOAD, encrypt(attr.askNoAppDownload))
        values.put(KEY_ACCOUNT_DETAILS_TIMESTAMP, encrypt(attr.accountDetailsTimeStamp))
        values.put(
            KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP,
            encrypt(attr.extendedAccountDetailsTimeStamp)
        )
        values.put(KEY_INVALIDATE_SDK_CACHE, encrypt(attr.invalidateSdkCache))
        values.put(KEY_USE_HTTPS_ONLY, encrypt(attr.useHttpsOnly))
        values.put(KEY_USE_HTTPS_ONLY, encrypt(attr.useHttpsOnly))
        values.put(KEY_SHOW_COPYRIGHT, encrypt(attr.showCopyright))
        values.put(KEY_SHOW_NOTIF_OFF, encrypt(attr.showNotifOff))
        values.put(KEY_LAST_PUBLIC_HANDLE, encrypt(attr.lastPublicHandle.toString()))
        values.put(
            KEY_LAST_PUBLIC_HANDLE_TIMESTAMP,
            encrypt(attr.lastPublicHandleTimeStamp.toString())
        )
        values.put(
            KEY_STORAGE_STATE,
            encrypt(storageStateIntMapper(attr.storageState).toString())
        )
        values.put(
            KEY_LAST_PUBLIC_HANDLE_TYPE,
            encrypt(attr.lastPublicHandleType.toString())
        )
        values.put(
            KEY_MY_CHAT_FILES_FOLDER_HANDLE,
            encrypt(attr.myChatFilesFolderHandle.toString())
        )
        values.put(KEY_TRANSFER_QUEUE_STATUS, encrypt(attr.transferQueueStatus))
        db.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
    }

    /**
     * Gets attributes.
     *
     * @param db Current DB.
     * @return The attributes.
     */
    private fun getAttributes(db: SupportSQLiteDatabase): MegaAttributes? {
        var attr: MegaAttributes? = null
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        try {
            db.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val online = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ATTR_ONLINE)))
                    val intents =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_ATTR_INTENTS)))
                    val askSizeDownload = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_ATTR_ASK_SIZE_DOWNLOAD
                            )
                        )
                    )
                    val askNoAppDownload = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_ATTR_ASK_NOAPP_DOWNLOAD
                            )
                        )
                    )
                    val accountDetailsTimeStamp = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_ACCOUNT_DETAILS_TIMESTAMP
                            )
                        )
                    )
                    val extendedAccountDetailsTimeStamp = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP
                            )
                        )
                    )
                    val invalidateSdkCache =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_INVALIDATE_SDK_CACHE)))
                    val useHttpsOnly =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_USE_HTTPS_ONLY)))
                    val showCopyright =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_COPYRIGHT)))
                    val showNotifOff =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_NOTIF_OFF)))
                    val lastPublicHandle =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_PUBLIC_HANDLE)))
                    val lastPublicHandleTimeStamp = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_LAST_PUBLIC_HANDLE_TIMESTAMP
                            )
                        )
                    )
                    val storageState =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_STORAGE_STATE)))
                    val lastPublicHandleType = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_LAST_PUBLIC_HANDLE_TYPE
                            )
                        )
                    )
                    val myChatFilesFolderHandle = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_MY_CHAT_FILES_FOLDER_HANDLE
                            )
                        )
                    )
                    val transferQueueStatus =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_TRANSFER_QUEUE_STATUS)))
                    attr = MegaAttributes(
                        online,
                        intents?.toIntOrNull() ?: 0,
                        askSizeDownload,
                        askNoAppDownload,
                        accountDetailsTimeStamp,
                        extendedAccountDetailsTimeStamp,
                        invalidateSdkCache,
                        useHttpsOnly,
                        showCopyright,
                        showNotifOff,
                        lastPublicHandle,
                        lastPublicHandleTimeStamp,
                        lastPublicHandleType?.toIntOrNull() ?: MegaApiJava.AFFILIATE_TYPE_INVALID,
                        storageState?.toIntOrNull()?.let { storageStateMapper(it) }
                            ?: StorageState.Unknown,
                        myChatFilesFolderHandle,
                        transferQueueStatus
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return attr
    }
    /**
     * Gets attributes.
     *
     * @return The attributes.
     */
    /**
     * Saves attributes in DB.
     *
     * @param attr Attributes to save.
     */
    override var attributes: MegaAttributes?
        get() = getAttributes(writableDatabase)
        set(attr) {
            setAttributes(writableDatabase, attr)
        }

    override fun setNonContactFirstName(name: String?, handle: String?): Int {
        Timber.d("setContactName: %s %s", name, handle)
        val values = ContentValues().apply {
            put(KEY_NONCONTACT_FIRSTNAME, encrypt(name))
        }
        val rows = writableDatabase.update(
            TABLE_NON_CONTACTS,
            SQLiteDatabase.CONFLICT_REPLACE,
            values,
            "$KEY_NONCONTACT_HANDLE = '${encrypt(handle)}'",
            emptyArray()
        )
        if (rows == 0) {
            values.put(KEY_NONCONTACT_HANDLE, encrypt(handle))
            writableDatabase.insert(TABLE_NON_CONTACTS, SQLiteDatabase.CONFLICT_NONE, values)
        }
        return rows
    }

    override fun setNonContactLastName(lastName: String?, handle: String?): Int {
        val values = ContentValues().apply {
            put(KEY_NONCONTACT_LASTNAME, encrypt(lastName))
        }
        val rows = writableDatabase.update(
            TABLE_NON_CONTACTS,
            SQLiteDatabase.CONFLICT_REPLACE,
            values,
            "$KEY_NONCONTACT_HANDLE = '${encrypt(handle)}'",
            emptyArray()
        )
        if (rows == 0) {
            values.put(KEY_NONCONTACT_HANDLE, encrypt(handle))
            writableDatabase.insert(TABLE_NON_CONTACTS, SQLiteDatabase.CONFLICT_NONE, values)
        }
        return rows
    }

    override fun setNonContactEmail(email: String?, handle: String?): Int {
        val values = ContentValues().apply {
            put(KEY_NONCONTACT_EMAIL, encrypt(email))
        }
        val rows = writableDatabase.update(
            TABLE_NON_CONTACTS,
            SQLiteDatabase.CONFLICT_REPLACE,
            values,
            "$KEY_NONCONTACT_HANDLE = '${encrypt(handle)}'",
            emptyArray()
        )
        if (rows == 0) {
            values.put(KEY_NONCONTACT_HANDLE, encrypt(handle))
            writableDatabase.insert(TABLE_NON_CONTACTS, SQLiteDatabase.CONFLICT_NONE, values)
        }
        return rows
    }

    override fun findNonContactByHandle(handle: String): NonContactInfo? {
        Timber.d("findNONContactByHandle: %s", handle)
        val selectQuery =
            "SELECT * FROM $TABLE_NON_CONTACTS WHERE $KEY_NONCONTACT_HANDLE = '${encrypt(handle)}'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val fullName = decrypt(cursor.getString(2))
                    val firstName = decrypt(cursor.getString(3))
                    val lastName = decrypt(cursor.getString(4))
                    val email = decrypt(cursor.getString(5))
                    return NonContactInfo(
                        handle,
                        fullName,
                        firstName,
                        lastName,
                        email
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun findContactByHandle(handleParam: Long): Contact? =
        runBlocking { megaLocalRoomGateway.getContactByHandle(handleParam) }

    override fun findContactByEmail(mail: String?): Contact? =
        runBlocking { megaLocalRoomGateway.getContactByEmail(mail) }

    override fun exists(handle: Long): Boolean {
        //Get the foreign key of the node
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_HANDLE = '${encrypt(handle.toString())}'"
        try {
            return readableDatabase.query(selectQuery).use { it.moveToFirst() }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return false
    }

    @Deprecated(
        message = "MegaOffline has been deprecated in favour of OfflineInformation",
        replaceWith = ReplaceWith("getOfflineInformation(handle)"),
        level = DeprecationLevel.WARNING
    )
    override fun findByHandle(handle: Long): MegaOffline? {
        return findByHandle(handle.toString())
    }

    @Deprecated(
        message = "MegaOffline has been deprecated in favour of OfflineInformation",
        replaceWith = ReplaceWith("getOfflineInformation(handle)"),
        level = DeprecationLevel.WARNING
    )
    override fun findByHandle(handle: String?): MegaOffline? {
        //Get the foreign key of the node
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_HANDLE = '${encrypt(handle)}'"
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(0).toInt()
                    val nodeHandle = decrypt(cursor.getString(1))
                    val path = decrypt(cursor.getString(2))
                    val name = decrypt(cursor.getString(3))
                    val parent = cursor.getInt(4)
                    val type = decrypt(cursor.getString(5))
                    val incoming = cursor.getInt(6)
                    val handleIncoming = decrypt(cursor.getString(7))
                    return MegaOffline(
                        id,
                        nodeHandle.toString(),
                        path.toString(),
                        name.toString(),
                        parent,
                        type,
                        incoming,
                        handleIncoming.toString()
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun setFirstTime(firstTime: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_FIRST_LOGIN= '${encrypt(firstTime.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_FIRST_LOGIN, encrypt(firstTime.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setPreferredSortCloud(order: String?) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_SORT_CLOUD= '${encrypt(order)}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_PREFERRED_SORT_CLOUD, encrypt(order))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setPreferredSortCameraUpload(order: String?) {
        Timber.d("set sort camera upload order: %s", order)
        setStringValue(TABLE_PREFERENCES, KEY_PREFERRED_SORT_CAMERA_UPLOAD, order)
    }

    override fun setPreferredSortOthers(order: String?) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_SORT_OTHERS= '${encrypt(order)}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_PREFERRED_SORT_OTHERS, encrypt(order))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setLastCloudFolder(folderHandle: String) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_LAST_CLOUD_FOLDER_HANDLE= '${
                            encrypt(folderHandle)
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                    Timber.d(
                        "KEY_LAST_CLOUD_FOLDER_HANDLE UPLOAD FOLDER: %s",
                        UPDATE_PREFERENCES_TABLE
                    )
                } else {
                    values.put(KEY_LAST_CLOUD_FOLDER_HANDLE, encrypt(folderHandle))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setAccountDetailsTimeStamp() {
        setAccountDetailsTimeStamp(System.currentTimeMillis() / 1000)
    }

    override fun resetAccountDetailsTimeStamp() {
        setAccountDetailsTimeStamp(-1)
    }

    private fun setAccountDetailsTimeStamp(accountDetailsTimeStamp: Long) {
        Timber.d("setAccountDetailsTimeStamp")
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTE_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_ACCOUNT_DETAILS_TIMESTAMP= '${
                            encrypt(accountDetailsTimeStamp.toString())
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTE_TABLE)
                } else {
                    values.put(
                        KEY_ACCOUNT_DETAILS_TIMESTAMP,
                        encrypt(accountDetailsTimeStamp.toString())
                    )
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setExtendedAccountDetailsTimestamp() {
        Timber.d("setExtendedAccountDetailsTimestamp")
        val extendedAccountDetailsTimestamp = System.currentTimeMillis() / 1000
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTE_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP= '${
                            encrypt(extendedAccountDetailsTimestamp.toString())
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTE_TABLE)
                } else {
                    values.put(
                        KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP,
                        encrypt(extendedAccountDetailsTimestamp.toString())
                    )
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun resetExtendedAccountDetailsTimestamp() {
        Timber.d("resetExtendedAccountDetailsTimestamp")
        val extendedAccountDetailsTimestamp: Long = -1
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTE_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP= '${
                            encrypt(extendedAccountDetailsTimestamp.toString())
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTE_TABLE)
                } else {
                    values.put(
                        KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP,
                        encrypt(extendedAccountDetailsTimestamp.toString())
                    )
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    /**
     * Set an integer value into the database.
     *
     * @param tableName  Name of the database's table.
     * @param columnName Name of the table's column.
     * @param value      Value to set.
     */
    private fun setIntValue(tableName: String, columnName: String, value: Int) {
        setStringValue(tableName, columnName, value.toString())
    }

    /**
     * Get an integer value from the database.
     *
     * @param tableName    Name of the database's table.
     * @param columnName   Name of the table's column.
     * @param defaultValue Default value to return if no result found.
     * @return Integer value selected from the database.
     */
    private fun getIntValue(tableName: String, columnName: String, defaultValue: Int): Int =
        getStringValue(tableName, columnName, defaultValue.toString())?.toIntOrNull()
            ?: defaultValue

    /**
     * Set a long value into the database.
     *
     * @param tableName  Name of the database's table.
     * @param columnName Name of the table's column.
     * @param value      Value to set.
     */
    private fun setLongValue(tableName: String, columnName: String, value: Long) {
        setStringValue(tableName, columnName, value.toString())
    }

    /**
     * Get a long value from the database.
     *
     * @param tableName    Name of the database's table.
     * @param columnName   Name of the table's column.
     * @param defaultValue Default value to return if no result found.
     * @return Long value selected from the database.
     */
    private fun getLongValue(tableName: String, columnName: String, defaultValue: Long): Long {
        try {
            val value = getStringValue(tableName, columnName, defaultValue.toString())
            if (!TextUtil.isTextEmpty(value)) {
                return value!!.toLong()
            }
        } catch (e: Exception) {
            Timber.w(e, "EXCEPTION - Return default value: %s", defaultValue)
        }
        return defaultValue
    }

    /**
     * Set a String value into the database.
     *
     * @param tableName  Name of the database's table.
     * @param columnName Name of the table's column.
     * @param value      Value to set.
     */
    private fun setStringValue(tableName: String, columnName: String, value: String?) {
        if (TextUtil.isTextEmpty(value)) {
            Timber.w("Set %s with empty value!", columnName)
        }
        val selectQuery = "SELECT * FROM $tableName"
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_TABLE =
                        "UPDATE $tableName SET $columnName= '${encrypt(value)}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_TABLE)
                } else {
                    val values = ContentValues()
                    values.put(columnName, encrypt(value))
                    writableDatabase.insert(tableName, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    /**
     * Get a String value from the database.
     *
     * @param tableName    Name of the database's table.
     * @param columnName   Name of the table's column.
     * @param defaultValue Default value to return if no result found.
     * @return String value selected from the database.
     */
    private fun getStringValue(
        tableName: String,
        columnName: String,
        defaultValue: String,
    ): String? {
        var value: String? = defaultValue
        val selectQuery =
            "SELECT $columnName FROM $tableName WHERE $KEY_ID = '1'"
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    value = decrypt(cursor.getString(0))
                    Timber.d("%s value: %s", columnName, value)
                } else {
                    Timber.w("No value found, setting default")
                    val values = ContentValues()
                    values.put(columnName, encrypt(defaultValue))
                    writableDatabase.insert(tableName, SQLiteDatabase.CONFLICT_NONE, values)
                    Timber.d("Default value: %s", defaultValue)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return value
    }

    /**
     * Get a boolean value from the database.
     *
     * @param tableName    Name of the database's table.
     * @param columnName   Name of the table's column.
     * @param defaultValue Default value to return if no result found.
     * @return Boolean value selected from the database.
     */
    private fun getBooleanValue(
        tableName: String,
        columnName: String,
        defaultValue: Boolean,
    ): Boolean =
        getStringValue(tableName, columnName, defaultValue.toString())?.toBooleanStrictOrNull()
            ?: defaultValue

    override var isPasscodeLockEnabled: Boolean
        get() = getBooleanValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_ENABLED, false)
        set(passcodeLockEnabled) {
            val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
            val values = ContentValues()
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val UPDATE_PREFERENCES_TABLE =
                            "UPDATE $TABLE_PREFERENCES SET $KEY_PASSCODE_LOCK_ENABLED= '${
                                encrypt(passcodeLockEnabled.toString())
                            }' WHERE $KEY_ID = '1'"
                        writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                    } else {
                        values.put(
                            KEY_PASSCODE_LOCK_ENABLED,
                            encrypt(passcodeLockEnabled.toString())
                        )
                        writableDatabase.insert(
                            TABLE_PREFERENCES,
                            SQLiteDatabase.CONFLICT_NONE,
                            values
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
        }

    override var passcodeLockCode: String
        get() = getStringValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_CODE, "")!!
        set(passcodeLockCode: String) {
            val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
            val values = ContentValues()
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val UPDATE_PREFERENCES_TABLE =
                            "UPDATE $TABLE_PREFERENCES SET $KEY_PASSCODE_LOCK_CODE= '${
                                encrypt(
                                    passcodeLockCode
                                )
                            }' WHERE $KEY_ID = '1'"
                        writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                    } else {
                        values.put(KEY_PASSCODE_LOCK_CODE, encrypt(passcodeLockCode))
                        writableDatabase.insert(
                            TABLE_PREFERENCES,
                            SQLiteDatabase.CONFLICT_NONE,
                            values
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
        }

    /**
     * Checks if the fingerprint lock setting is enabled.
     *
     * @return True if the fingerprint is enabled, false otherwise.
     */
    /**
     * Sets if the fingerprint lock setting is enabled or not.
     *
     * @param enabled True if the fingerprint is enabled, false otherwise.
     */
    override var isFingerprintLockEnabled: Boolean
        get() = getBooleanValue(TABLE_PREFERENCES, KEY_FINGERPRINT_LOCK, false)
        set(enabled) {
            setStringValue(TABLE_PREFERENCES, KEY_FINGERPRINT_LOCK, "" + enabled)
        }

    override fun setStorageAskAlways(storageAskAlways: Boolean) {
        setStringValue(TABLE_PREFERENCES, KEY_STORAGE_ASK_ALWAYS, storageAskAlways.toString())
    }
    /**
     * Gets the flag which indicates if should ask the user about set the current path as default download location.
     *
     * @return true if should ask, false otherwise.
     */
    /**
     * Sets the flag to indicate if should ask the user about set the current path as default download location.
     *
     * @param askSetDownloadLocation true if should ask, false otherwise.
     */
    override var askSetDownloadLocation: Boolean
        get() = getBooleanValue(TABLE_PREFERENCES, KEY_ASK_SET_DOWNLOAD_LOCATION, true)
        set(askSetDownloadLocation) {
            setStringValue(
                TABLE_PREFERENCES,
                KEY_ASK_SET_DOWNLOAD_LOCATION,
                askSetDownloadLocation.toString()
            )
        }

    override fun setStorageDownloadLocation(storageDownloadLocation: String?) {
        if (storageDownloadLocation == null) return

        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_STORAGE_DOWNLOAD_LOCATION= '${
                            encrypt(storageDownloadLocation)
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(storageDownloadLocation))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setAttrAskSizeDownload(askSizeDownload: String?) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_ASK_SIZE_DOWNLOAD='${
                            encrypt(askSizeDownload)
                        }' WHERE $KEY_ID ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_ATTR_ASK_SIZE_DOWNLOAD, encrypt(askSizeDownload))
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setUseHttpsOnly(useHttpsOnly: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_USE_HTTPS_ONLY='${encrypt(useHttpsOnly.toString())}' WHERE $KEY_ID ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_USE_HTTPS_ONLY, encrypt(useHttpsOnly.toString()))
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override val useHttpsOnly: String?
        get() {
            val selectQuery =
                "SELECT $KEY_USE_HTTPS_ONLY FROM $TABLE_ATTRIBUTES WHERE $KEY_ID = '1'"
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return decrypt(cursor.getString(0))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return "false"
        }

    override fun setShowCopyright(showCopyright: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_SHOW_COPYRIGHT='${encrypt(showCopyright.toString())}' WHERE $KEY_ID ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_SHOW_COPYRIGHT, encrypt(showCopyright.toString()))
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override val shouldShowCopyright: Boolean
        get() {
            val selectQuery =
                "SELECT $KEY_SHOW_COPYRIGHT FROM $TABLE_ATTRIBUTES WHERE $KEY_ID = '1'"
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return decrypt(cursor.getString(0))?.toBoolean() ?: true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return true
        }

    override fun setShowNotifOff(showNotifOff: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_SHOW_NOTIF_OFF='${encrypt(showNotifOff.toString())}' WHERE $KEY_ID ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_SHOW_NOTIF_OFF, encrypt(showNotifOff.toString()))
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setLastPublicHandle(handle: Long) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_LAST_PUBLIC_HANDLE= '${encrypt(handle.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_LAST_PUBLIC_HANDLE, encrypt(handle.toString()))
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setLastPublicHandleTimeStamp(lastPublicHandleTimeStamp: Long) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTE_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_LAST_PUBLIC_HANDLE_TIMESTAMP= '${
                            encrypt(lastPublicHandleTimeStamp.toString())
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTE_TABLE)
                } else {
                    values.put(
                        KEY_LAST_PUBLIC_HANDLE_TIMESTAMP,
                        encrypt(lastPublicHandleTimeStamp.toString())
                    )
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setLastPublicHandleTimeStamp() {
        Timber.d("setLastPublicHandleTimeStamp")
        val lastPublicHandleTimeStamp = System.currentTimeMillis() / 1000
        setLastPublicHandleTimeStamp(lastPublicHandleTimeStamp)
    }
    /**
     * Get the last public handle type value from the database.
     *
     * @return Last public handle type value.
     */
    /**
     * Set the last public handle type value into the database.
     *
     * @param lastPublicHandleType Last public handle type value.
     */
    override var lastPublicHandleType: Int
        get() {
            Timber.i("Getting the last public handle type from DB")
            return getIntValue(
                TABLE_ATTRIBUTES,
                KEY_LAST_PUBLIC_HANDLE_TYPE,
                MegaApiJava.AFFILIATE_TYPE_INVALID
            )
        }
        set(lastPublicHandleType) {
            Timber.i("Setting the last public handle type in the DB")
            setIntValue(TABLE_ATTRIBUTES, KEY_LAST_PUBLIC_HANDLE_TYPE, lastPublicHandleType)
        }

    /**
     * Get the handle of "My chat files" folder from the database.
     *
     * @return Handle value.
     */
    /**
     * Set the handle of "My chat files" folder into the database.
     *
     * @param myChatFilesFolderHandle Handle value.
     */
    override var myChatFilesFolderHandle: Long
        get() {
            Timber.i("Getting the storage state from DB")
            return getLongValue(
                TABLE_ATTRIBUTES,
                KEY_MY_CHAT_FILES_FOLDER_HANDLE,
                MegaApiJava.INVALID_HANDLE
            )
        }
        set(myChatFilesFolderHandle) {
            Timber.i("Setting the storage state in the DB")
            setLongValue(TABLE_ATTRIBUTES, KEY_MY_CHAT_FILES_FOLDER_HANDLE, myChatFilesFolderHandle)
        }
    /**
     * Get the status of the transfer queue.
     *
     * @return True if the queue is paused, false otherwise.
     */
    /**
     * Set the status of the transfer queue.
     *
     * @param transferQueueStatus True if the queue is paused, false otherwise.
     */
    override var transferQueueStatus: Boolean
        get() {
            Timber.i("Getting the storage state from DB")
            return getBooleanValue(TABLE_ATTRIBUTES, KEY_TRANSFER_QUEUE_STATUS, false)
        }
        set(transferQueueStatus) {
            Timber.i("Setting the storage state in the DB")
            setStringValue(
                TABLE_ATTRIBUTES,
                KEY_TRANSFER_QUEUE_STATUS,
                transferQueueStatus.toString()
            )
        }

    override val showNotifOff: String?
        get() {
            val selectQuery =
                "SELECT $KEY_SHOW_NOTIF_OFF FROM $TABLE_ATTRIBUTES WHERE $KEY_ID = '1'"
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return decrypt(cursor.getString(0))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return "true"
        }

    override fun setInvalidateSdkCache(invalidateSdkCache: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_INVALIDATE_SDK_CACHE='" + encrypt(
                            invalidateSdkCache.toString()
                        ) + "' WHERE " + KEY_ID + " ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(
                        KEY_INVALIDATE_SDK_CACHE,
                        encrypt(invalidateSdkCache.toString())
                    )
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun clearCredentials() {
        Timber.w("Clear local credentials!")
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_CREDENTIALS")
    }

    override fun clearEphemeral() {
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_EPHEMERAL")
        legacyDatabaseMigration.onCreate(writableDatabase)
    }

    override fun clearPreferences() {
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_PREFERENCES")
        legacyDatabaseMigration.onCreate(writableDatabase)
    }

    override fun clearAttributes() {
        var lastPublicHandle: Long
        var lastPublicHandleTimeStamp: Long = -1
        var lastPublicHandleType = MegaApiJava.AFFILIATE_TYPE_INVALID
        try {
            val attributes = attributes
            lastPublicHandle = attributes!!.lastPublicHandle
            lastPublicHandleTimeStamp = attributes.lastPublicHandleTimeStamp
            lastPublicHandleType = attributes.lastPublicHandleType
        } catch (e: Exception) {
            Timber.w(e, "EXCEPTION getting last public handle info.")
            lastPublicHandle = MegaApiJava.INVALID_HANDLE
        }
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_ATTRIBUTES")
        legacyDatabaseMigration.onCreate(writableDatabase)
        if (lastPublicHandle != MegaApiJava.INVALID_HANDLE) {
            try {
                setLastPublicHandle(lastPublicHandle)
                setLastPublicHandleTimeStamp(lastPublicHandleTimeStamp)
                this.lastPublicHandleType = lastPublicHandleType
            } catch (e: Exception) {
                Timber.w(e, "EXCEPTION saving last public handle info.")
            }
        }
    }

    override fun clearContacts() {
        applicationScope.launch {
            megaLocalRoomGateway.deleteAllContacts()
        }
    }

    override fun clearNonContacts() {
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_NON_CONTACTS")
        legacyDatabaseMigration.onCreate(writableDatabase)
    }

    override fun clearChatItems() {
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_ITEMS")
        legacyDatabaseMigration.onCreate(writableDatabase)
    }

    override fun clearChatSettings() {
        writableDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_SETTINGS")
        legacyDatabaseMigration.onCreate(writableDatabase)
    }

    /**
     * Updates a pending message.
     *
     * @param idMessage   Identifier of the pending message.
     * @param transferTag Identifier of the transfer.
     */
    override fun updatePendingMessageOnTransferStart(idMessage: Long, transferTag: Int) {
        updatePendingMessage(
            idMessage,
            transferTag,
            Constants.INVALID_OPTION,
            PendingMessageState.UPLOADING.value
        )
    }

    /**
     * Updates a pending message.
     *
     * @param idMessage  Identifier of the pending message.
     * @param nodeHandle Handle of the node already uploaded.
     * @param state      State of the pending message.
     */
    override fun updatePendingMessageOnTransferFinish(
        idMessage: Long,
        nodeHandle: String?,
        state: Int,
    ) {
        updatePendingMessage(idMessage, Constants.INVALID_ID, nodeHandle, state)
    }

    /**
     * Updates a pending message.
     *
     * @param idMessage   Identifier of the pending message.
     * @param transferTag Identifier of the transfer.
     * @param nodeHandle  Handle of the node already uploaded.
     * @param state       State of the pending message.
     */
    override fun updatePendingMessage(
        idMessage: Long,
        transferTag: Int,
        nodeHandle: String?,
        state: Int,
    ) {
        val values = ContentValues()
        if (transferTag != Constants.INVALID_ID) {
            values.put(KEY_PENDING_MSG_TRANSFER_TAG, transferTag)
        }
        values.put(KEY_PENDING_MSG_NODE_HANDLE, encrypt(nodeHandle))
        values.put(KEY_PENDING_MSG_STATE, state)
        val where = "$KEY_ID=$idMessage"
        writableDatabase.update(
            TABLE_PENDING_MSG_SINGLE,
            SQLiteDatabase.CONFLICT_REPLACE,
            values,
            where,
            emptyArray()
        )
    }

    override fun removeSentPendingMessages() {
        Timber.d("removeSentPendingMessages")
        writableDatabase.delete(
            TABLE_PENDING_MSG_SINGLE,
            KEY_PENDING_MSG_STATE + "=" + PendingMessageState.SENT.value,
            emptyArray()
        )
    }

    override fun removePendingMessageByChatId(idChat: Long) {
        Timber.d("removePendingMessageByChatId")
        writableDatabase.delete(
            TABLE_PENDING_MSG_SINGLE,
            "$KEY_PENDING_MSG_ID_CHAT = '${encrypt(idChat.toString())}'",
            emptyArray()
        )
    }

    override val autoPlayEnabled: String?
        get() {
            val selectQuery =
                "SELECT $KEY_AUTO_PLAY FROM $TABLE_PREFERENCES WHERE $KEY_ID = '1'"
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return decrypt(cursor.getString(0))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return "false"
        }
    override var sdCardUri: String?
        get() = getStringValue(TABLE_PREFERENCES, KEY_SD_CARD_URI, "")
        set(sdCardUri) {
            setStringValue(TABLE_PREFERENCES, KEY_SD_CARD_URI, sdCardUri)
        }

    override fun setAutoPlayEnabled(enabled: String) {
        Timber.d("setAutoPlayEnabled")
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_AUTO_PLAY='${encrypt(enabled)}' WHERE $KEY_ID ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_AUTO_PLAY, encrypt(enabled))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    /**
     * Get the index of a column in a cursor.
     * Avoid to access column with hardcode index.
     *
     * @param cursor     Cursor object which has the column.
     * @param columnName Name of the column.
     * @return The index of the column in the cursor.
     */
    private fun getColumnIndex(cursor: Cursor, columnName: String): Int {
        return cursor.getColumnIndex(columnName)
    }

    companion object {
        const val TABLE_PREFERENCES = "preferences"
        const val TABLE_CREDENTIALS = "credentials"
        const val TABLE_ATTRIBUTES = "attributes"
        const val TABLE_CHAT_ITEMS = "chat"
        const val TABLE_NON_CONTACTS = "noncontacts"
        const val TABLE_CHAT_SETTINGS = "chatsettings"
        const val TABLE_COMPLETED_TRANSFERS = "completedtransfers"
        const val TABLE_EPHEMERAL = "ephemeral"
        const val TABLE_PENDING_MSG_SINGLE = "pendingmsgsingle"
        const val KEY_ID = "id"
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"
        const val KEY_SESSION = "session"
        const val KEY_FIRST_NAME = "firstname"
        const val KEY_LAST_NAME = "lastname"
        const val KEY_MY_HANDLE = "myhandle"
        const val KEY_FIRST_LOGIN = "firstlogin"
        const val KEY_CAM_SYNC_ENABLED = "camsyncenabled"
        const val KEY_SEC_FOLDER_ENABLED = "secondarymediafolderenabled"
        const val KEY_SEC_FOLDER_HANDLE = "secondarymediafolderhandle"
        const val KEY_SEC_FOLDER_LOCAL_PATH = "secondarymediafolderlocalpath"
        const val KEY_CAM_SYNC_HANDLE = "camsynchandle"
        const val KEY_CAM_SYNC_WIFI = "wifi"
        const val KEY_CAM_SYNC_LOCAL_PATH = "camsynclocalpath"
        const val KEY_CAM_SYNC_FILE_UPLOAD = "fileUpload"
        const val KEY_CAM_SYNC_TIMESTAMP = "camSyncTimeStamp"
        const val KEY_CAM_VIDEO_SYNC_TIMESTAMP = "camVideoSyncTimeStamp"
        const val KEY_UPLOAD_VIDEO_QUALITY = "uploadVideoQuality"
        const val KEY_CONVERSION_ON_CHARGING = "conversionOnCharging"
        const val KEY_REMOVE_GPS = "removeGPS"
        const val KEY_CHARGING_ON_SIZE = "chargingOnSize"
        const val KEY_SHOULD_CLEAR_CAMSYNC_RECORDS = "shouldclearcamsyncrecords"
        const val KEY_KEEP_FILE_NAMES = "keepFileNames"
        const val KEY_SHOW_INVITE_BANNER = "showinvitebanner"
        const val KEY_ASK_FOR_DISPLAY_OVER = "askfordisplayover"
        const val KEY_PASSCODE_LOCK_ENABLED = "pinlockenabled"
        const val KEY_PASSCODE_LOCK_TYPE = "pinlocktype"
        const val KEY_PASSCODE_LOCK_CODE = "pinlockcode"
        const val KEY_PASSCODE_LOCK_REQUIRE_TIME = "passcodelockrequiretime"
        const val KEY_FINGERPRINT_LOCK = "fingerprintlock"
        const val KEY_STORAGE_ASK_ALWAYS = "storageaskalways"
        const val KEY_STORAGE_DOWNLOAD_LOCATION = "storagedownloadlocation"
        const val KEY_LAST_UPLOAD_FOLDER = "lastuploadfolder"
        const val KEY_LAST_CLOUD_FOLDER_HANDLE = "lastcloudfolder"
        const val KEY_ATTR_ONLINE = "online"
        const val KEY_ATTR_INTENTS = "intents"
        const val KEY_ATTR_ASK_SIZE_DOWNLOAD = "asksizedownload"
        const val KEY_ATTR_ASK_NOAPP_DOWNLOAD = "asknoappdownload"
        const val KEY_OFF_HANDLE = "handle"
        const val KEY_OFF_PATH = "path"
        const val KEY_OFF_NAME = "name"
        const val KEY_OFF_PARENT = "parentId"
        const val KEY_OFF_TYPE = "type"
        const val KEY_OFF_INCOMING = "incoming"
        const val KEY_OFF_HANDLE_INCOMING = "incomingHandle"
        const val KEY_OFF_LAST_MODIFIED_TIME = "lastModifiedTime"
        const val KEY_SEC_SYNC_TIMESTAMP = "secondarySyncTimeStamp"
        const val KEY_SEC_VIDEO_SYNC_TIMESTAMP = "secondaryVideoSyncTimeStamp"
        const val KEY_STORAGE_ADVANCED_DEVICES = "storageadvanceddevices"
        const val KEY_ASK_SET_DOWNLOAD_LOCATION = "askSetDefaultDownloadLocation"
        const val KEY_PREFERRED_VIEW_LIST = "preferredviewlist"
        const val KEY_PREFERRED_VIEW_LIST_CAMERA = "preferredviewlistcamera"
        const val KEY_URI_EXTERNAL_SD_CARD = "uriexternalsdcard"
        const val KEY_URI_MEDIA_EXTERNAL_SD_CARD = "urimediaexternalsdcard"
        const val KEY_SD_CARD_URI = "sdcarduri"
        const val KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD = "camerafolderexternalsdcard"
        const val KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD = "mediafolderexternalsdcard"
        const val KEY_CONTACT_HANDLE = "handle"
        const val KEY_CONTACT_MAIL = "mail"
        const val KEY_CONTACT_NAME = "name"
        const val KEY_CONTACT_LAST_NAME = "lastname"
        const val KEY_CONTACT_NICKNAME = "nickname"
        const val KEY_PREFERRED_SORT_CLOUD = "preferredsortcloud"
        const val KEY_PREFERRED_SORT_CAMERA_UPLOAD = "preferredsortcameraupload"
        const val KEY_PREFERRED_SORT_OTHERS = "preferredsortothers"
        const val KEY_USE_HTTPS_ONLY = "usehttpsonly"
        const val KEY_SHOW_COPYRIGHT = "showcopyright"
        const val KEY_SHOW_NOTIF_OFF = "shownotifoff"
        const val KEY_ACCOUNT_DETAILS_TIMESTAMP = "accountdetailstimestamp"

        @Deprecated("Unused database properties")
        const val KEY_PAYMENT_METHODS_TIMESTAMP = "paymentmethodsstimestamp"

        @Deprecated("Unused database properties")
        const val KEY_PRICING_TIMESTAMP = "pricingtimestamp"
        const val KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP = "extendedaccountdetailstimestamp"
        const val KEY_CHAT_HANDLE = "chathandle"
        const val KEY_CHAT_ITEM_NOTIFICATIONS = "chatitemnotifications"
        const val KEY_CHAT_ITEM_RINGTONE = "chatitemringtone"
        const val KEY_CHAT_ITEM_SOUND_NOTIFICATIONS = "chatitemnotificationsound"
        const val KEY_CHAT_ITEM_WRITTEN_TEXT = "chatitemwrittentext"
        const val KEY_CHAT_ITEM_EDITED_MSG_ID = "chatitemeditedmsgid"
        const val KEY_NONCONTACT_HANDLE = "noncontacthandle"
        const val KEY_NONCONTACT_FULLNAME = "noncontactfullname"
        const val KEY_NONCONTACT_FIRSTNAME = "noncontactfirstname"
        const val KEY_NONCONTACT_LASTNAME = "noncontactlastname"
        const val KEY_NONCONTACT_EMAIL = "noncontactemail"
        const val KEY_CHAT_NOTIFICATIONS_ENABLED = "chatnotifications"
        const val KEY_CHAT_SOUND_NOTIFICATIONS = "chatnotificationsound"
        const val KEY_CHAT_VIBRATION_ENABLED = "chatvibrationenabled"
        const val KEY_CHAT_VIDEO_QUALITY = "chatvideoQuality"
        const val KEY_INVALIDATE_SDK_CACHE = "invalidatesdkcache"
        const val KEY_TRANSFER_FILENAME = "transferfilename"
        const val KEY_TRANSFER_TYPE = "transfertype"
        const val KEY_TRANSFER_STATE = "transferstate"
        const val KEY_TRANSFER_SIZE = "transfersize"
        const val KEY_TRANSFER_HANDLE = "transferhandle"
        const val KEY_TRANSFER_PATH = "transferpath"
        const val KEY_TRANSFER_OFFLINE = "transferoffline"
        const val KEY_TRANSFER_TIMESTAMP = "transfertimestamp"
        const val KEY_TRANSFER_ERROR = "transfererror"
        const val KEY_TRANSFER_ORIGINAL_PATH = "transferoriginalpath"
        const val KEY_TRANSFER_PARENT_HANDLE = "transferparenthandle"
        const val KEY_FIRST_LOGIN_CHAT = "firstloginchat"
        const val KEY_AUTO_PLAY = "autoplay"
        const val KEY_ID_CHAT = "idchat"

        const val KEY_LAST_PUBLIC_HANDLE = "lastpublichandle"
        const val KEY_LAST_PUBLIC_HANDLE_TIMESTAMP = "lastpublichandletimestamp"
        const val KEY_LAST_PUBLIC_HANDLE_TYPE = "lastpublichandletype"
        const val KEY_STORAGE_STATE = "storagestate"
        const val KEY_MY_CHAT_FILES_FOLDER_HANDLE = "mychatfilesfolderhandle"
        const val KEY_TRANSFER_QUEUE_STATUS = "transferqueuestatus"
        const val KEY_PENDING_MSG_ID_CHAT = "idchat"
        const val KEY_PENDING_MSG_TIMESTAMP = "timestamp"
        const val KEY_PENDING_MSG_TEMP_KARERE = "idtempkarere"
        const val KEY_PENDING_MSG_FILE_PATH = "filePath"
        const val KEY_PENDING_MSG_NAME = "filename"
        const val KEY_PENDING_MSG_NODE_HANDLE = "nodehandle"
        const val KEY_PENDING_MSG_FINGERPRINT = "filefingerprint"
        const val KEY_PENDING_MSG_TRANSFER_TAG = "transfertag"
        const val KEY_PENDING_MSG_STATE = "state"

        const val KEY_SD_TRANSFERS_TAG = "sdtransfertag"
        const val KEY_SD_TRANSFERS_NAME = "sdtransfername"
        const val KEY_SD_TRANSFERS_SIZE = "sdtransfersize"
        const val KEY_SD_TRANSFERS_HANDLE = "sdtransferhandle"
        const val KEY_SD_TRANSFERS_APP_DATA = "sdtransferappdata"
        const val KEY_SD_TRANSFERS_PATH = "sdtransferpath"
        const val CREATE_SD_TRANSFERS_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_SD_TRANSFERS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +                     // 0
                    "$KEY_SD_TRANSFERS_TAG INTEGER, " +                   // 1
                    "$KEY_SD_TRANSFERS_NAME TEXT, " +                     // 2
                    "$KEY_SD_TRANSFERS_SIZE TEXT, " +                     // 3
                    "$KEY_SD_TRANSFERS_HANDLE TEXT, " +                   // 4
                    "$KEY_SD_TRANSFERS_PATH TEXT, " +                     // 5
                    "$KEY_SD_TRANSFERS_APP_DATA TEXT)"                    // 6
        const val OLD_VIDEO_QUALITY_ORIGINAL = 0

        fun encrypt(original: String?): String? =
            original?.let {
                try {
                    val encrypted = Util.aes_encrypt(aesKey, it.toByteArray())
                    Base64.encodeToString(encrypted, Base64.DEFAULT)
                } catch (e: Exception) {
                    Timber.e(e, "Error encrypting DB field")
                    e.printStackTrace()
                    null
                }
            }

        private val aesKey: ByteArray
            get() {
                val key = Settings.Secure.ANDROID_ID + "fkvn8 w4y*(NC\$G*(G($*GR*(#)*huio4h389\$G"
                return key.toByteArray().copyOfRange(0, 32)
            }

        fun decrypt(encodedString: String?): String? =
            encodedString?.let {
                try {
                    val encoded = Base64.decode(encodedString, Base64.DEFAULT)
                    val original = Util.aes_decrypt(aesKey, encoded)
                    String(original)
                } catch (e: Exception) {
                    Timber.e(e, "Error decrypting DB field")
                    e.printStackTrace()
                    null
                }
            }

    }
}
