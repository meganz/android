package mega.privacy.android.app

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.main.megachat.ChatItemPreferences
import mega.privacy.android.app.monitoring.CrashReporter
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.OfflineUtils
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
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_ON
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.Locale
import javax.inject.Inject


/**
 * Sqlite implementation of database handler
 */
class SqliteDatabaseHandler @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val crashReporter: CrashReporter,
    private val legacyLoggingSettings: LegacyLoggingSettings,
    private val storageStateMapper: StorageStateMapper,
    private val storageStateIntMapper: StorageStateIntMapper,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    private val sqLiteOpenHelper: SupportSQLiteOpenHelper,
    private val legacyDatabaseMigration: LegacyDatabaseMigration,
) : LegacyDatabaseHandler {
    private val writableDatabase: SupportSQLiteDatabase by lazy { sqLiteOpenHelper.writableDatabase }
    private val readableDatabase: SupportSQLiteDatabase by lazy { sqLiteOpenHelper.readableDatabase }

    override fun saveCredentials(userCredentials: UserCredentials) {
        val values = ContentValues().apply {
            with(userCredentials) {
                put(KEY_ID, 1)
                email?.let { put(KEY_EMAIL, encrypt(it)) }
                session?.let { put(KEY_SESSION, encrypt(it)) }
                myHandle?.let { put(KEY_MY_HANDLE, encrypt(it)) }
            }
        }
        writableDatabase.insert(TABLE_CREDENTIALS, SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    override fun shouldClearCamsyncRecords(): Boolean {
        val selectQuery =
            "SELECT $KEY_SHOULD_CLEAR_CAMSYNC_RECORDS FROM $TABLE_PREFERENCES"
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    var should = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            KEY_SHOULD_CLEAR_CAMSYNC_RECORDS
                        )
                    )
                    should = decrypt(should)
                    return if (should.isNullOrEmpty()) {
                        false
                    } else {
                        java.lang.Boolean.valueOf(should)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return false
    }

    override fun saveShouldClearCamsyncRecords(should: Boolean) {
        val sql =
            "UPDATE $TABLE_PREFERENCES SET $KEY_SHOULD_CLEAR_CAMSYNC_RECORDS = '${encrypt(should.toString())}'"
        writableDatabase.execSQL(sql)
    }

    override fun findMaxTimestamp(isSecondary: Boolean, fileType: Int): Long? {
        val selectQuery = "SELECT $KEY_SYNC_TIMESTAMP FROM $TABLE_SYNC_RECORDS  " +
                "WHERE $KEY_SYNC_SECONDARY = '${encrypt(isSecondary.toString())}' " +
                "AND $KEY_SYNC_TYPE = $fileType"
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val timestamps: MutableList<Long> = ArrayList(cursor.count)
                    do {
                        val timestamp = decrypt(cursor.getString(0))
                        if (timestamp == null) {
                            timestamps.add(0L)
                        } else {
                            timestamps.add(java.lang.Long.valueOf(timestamp))
                        }
                    } while (cursor.moveToNext())
                    if (timestamps.isEmpty()) {
                        return null
                    }
                    Collections.sort(timestamps, java.util.Comparator { o1, o2 ->
                        if (o1 == o2) {
                            return@Comparator 0
                        }
                        if (o1 > o2) -1 else 1
                    })
                    return timestamps[0]
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun setCameraUploadVideoQuality(quality: Int) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_UPLOAD_VIDEO_QUALITY= '${encrypt(quality.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_UPLOAD_VIDEO_QUALITY, encrypt(quality.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setConversionOnCharging(onCharging: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CONVERSION_ON_CHARGING= '${
                            encrypt(onCharging.toString())
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CONVERSION_ON_CHARGING, encrypt(onCharging.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setChargingOnSize(size: Int) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CHARGING_ON_SIZE= '${encrypt(size.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CHARGING_ON_SIZE, encrypt(size.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setRemoveGPS(removeGPS: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_REMOVE_GPS= '${encrypt(removeGPS.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_REMOVE_GPS, encrypt(removeGPS.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun saveMyEmail(email: String?) {
        Timber.d("saveEmail: %s", email)
        val selectQuery = "SELECT * FROM $TABLE_CREDENTIALS"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_CREDENTIALS_TABLE =
                        "UPDATE $TABLE_CREDENTIALS SET $KEY_EMAIL= '${encrypt(email)}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_CREDENTIALS_TABLE)
                } else {
                    values.put(KEY_EMAIL, encrypt(email))
                    writableDatabase.insert(TABLE_CREDENTIALS, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun saveMyFirstName(firstName: String?) {
        val selectQuery = "SELECT * FROM $TABLE_CREDENTIALS"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_CREDENTIALS_TABLE =
                        "UPDATE $TABLE_CREDENTIALS SET $KEY_FIRST_NAME= '${encrypt(firstName)}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_CREDENTIALS_TABLE)
                } else {
                    values.put(KEY_FIRST_NAME, encrypt(firstName))
                    writableDatabase.insert(TABLE_CREDENTIALS, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun saveMyLastName(lastName: String?) {
        val selectQuery = "SELECT * FROM $TABLE_CREDENTIALS"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_CREDENTIALS_TABLE =
                        "UPDATE $TABLE_CREDENTIALS SET $KEY_LAST_NAME= '${encrypt(lastName)}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_CREDENTIALS_TABLE)
                } else {
                    values.put(KEY_LAST_NAME, encrypt(lastName))
                    writableDatabase.insert(TABLE_CREDENTIALS, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override val myEmail: String?
        get() {
            val selectQuery = "SELECT $KEY_EMAIL FROM $TABLE_CREDENTIALS"
            var email: String? = null
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        email = decrypt(cursor.getString(0))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return email
        }

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

    override fun shouldAskForDisplayOver(): Boolean {
        var should = true
        val text = getStringValue(TABLE_PREFERENCES, KEY_ASK_FOR_DISPLAY_OVER, "")
        if (!TextUtils.isEmpty(text)) {
            should = text.toBoolean()
        }
        return should
    }

    override fun dontAskForDisplayOver() {
        writableDatabase.execSQL(
            "UPDATE $TABLE_PREFERENCES SET $KEY_ASK_FOR_DISPLAY_OVER = '${
                encrypt(
                    "false"
                )
            }';"
        )
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
                    val shouldClearCameraSyncRecords = decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                KEY_SHOULD_CLEAR_CAMSYNC_RECORDS
                            )
                        )
                    )
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
                        shouldClearCameraSyncRecords,
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

    override fun setChatItemPreferences(chatPrefs: ChatItemPreferences) {
        val values = ContentValues().apply {
            put(KEY_CHAT_HANDLE, encrypt(chatPrefs.chatHandle))
            put(KEY_CHAT_ITEM_NOTIFICATIONS, "")
            put(KEY_CHAT_ITEM_RINGTONE, "")
            put(KEY_CHAT_ITEM_SOUND_NOTIFICATIONS, "")
            put(KEY_CHAT_ITEM_WRITTEN_TEXT, encrypt(chatPrefs.writtenText))
            put(KEY_CHAT_ITEM_EDITED_MSG_ID, encrypt(chatPrefs.editedMsgId))
        }

        writableDatabase.insert(TABLE_CHAT_ITEMS, SQLiteDatabase.CONFLICT_NONE, values)
    }

    override fun setWrittenTextItem(handle: String?, text: String?, editedMsgId: String?): Int {
        Timber.d("setWrittenTextItem: %s %s", text, handle)
        val values = ContentValues().apply {
            put(KEY_CHAT_ITEM_WRITTEN_TEXT, encrypt(text))
            put(
                KEY_CHAT_ITEM_EDITED_MSG_ID,
                if (!TextUtil.isTextEmpty(editedMsgId)) encrypt(editedMsgId) else ""
            )
        }

        return writableDatabase.update(
            TABLE_CHAT_ITEMS,
            SQLiteDatabase.CONFLICT_REPLACE,
            values,
            "$KEY_CHAT_HANDLE = '${encrypt(handle)}'",
            null
        )
    }

    override fun findChatPreferencesByHandle(handle: String?): ChatItemPreferences? {
        Timber.d("findChatPreferencesByHandle: %s", handle)
        var prefs: ChatItemPreferences?
        val selectQuery =
            "SELECT * FROM $TABLE_CHAT_ITEMS WHERE $KEY_CHAT_HANDLE = '${encrypt(handle)}'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val chatHandle = decrypt(cursor.getString(1))
                    val writtenText = decrypt(cursor.getString(5))
                    val editedMsg = decrypt(cursor.getString(6))
                    prefs = if (!TextUtil.isTextEmpty(editedMsg))
                        ChatItemPreferences(chatHandle, writtenText, editedMsg)
                    else ChatItemPreferences(chatHandle, writtenText)
                    return prefs
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun areNotificationsEnabled(handle: String?): String? {
        val selectQuery =
            "SELECT * FROM $TABLE_CHAT_ITEMS WHERE $KEY_CHAT_HANDLE = '${encrypt(handle)}'"
        var result: String? = Constants.NOTIFICATIONS_ENABLED
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    result = decrypt(cursor.getString(2))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return result
    }

    /**
     * Extracts a completed transfer of a row.
     *
     * @param cursor Cursor from which the data should be extracted.
     * @return The extracted completed transfer.
     */
    private fun extractAndroidCompletedTransfer(cursor: Cursor): CompletedTransfer {
        val id = cursor.getInt(0)
        val filename = decrypt(cursor.getString(1)).orEmpty()
        val type = decrypt(cursor.getString(2))?.toInt() ?: -1
        val state = decrypt(cursor.getString(3))?.toInt() ?: -1
        val size = decrypt(cursor.getString(4)).orEmpty()
        val handle = decrypt(cursor.getString(5))?.toLong() ?: -1
        val path = decrypt(cursor.getString(6)).orEmpty()
        val offline = decrypt(cursor.getString(7))?.toBooleanStrictOrNull()
        val timestamp = decrypt(cursor.getString(8))?.toLong() ?: -1
        val error = decrypt(cursor.getString(9))
        val originalPath = decrypt(cursor.getString(10)).orEmpty()
        val parentHandle = decrypt(cursor.getString(11))?.toLong() ?: -1
        return CompletedTransfer(
            id, filename, type, state, size, handle, path,
            offline, timestamp, error, originalPath, parentHandle
        )
    }

    /**
     * Gets a list with completed transfers depending on the query received by parameter.
     *
     * @param selectQuery the query which selects specific completed transfers
     * @return The list with the completed transfers.
     */
    private fun getCompletedTransfers(selectQuery: String): ArrayList<CompletedTransfer> {
        val cTs = ArrayList<CompletedTransfer>()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToLast()) {
                    do {
                        cTs.add(extractAndroidCompletedTransfer(cursor))
                    } while (cursor.moveToPrevious())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return cTs
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
                    if (!legacyLoggingSettings.areSDKLogsEnabled() && cursor.getColumnIndex(
                            KEY_FILE_LOGGER_SDK
                        ) != Constants.INVALID_VALUE
                    ) {
                        val fileLoggerSDK =
                            decrypt(cursor.getString(getColumnIndex(cursor, KEY_FILE_LOGGER_SDK)))
                        legacyLoggingSettings.updateSDKLogs(
                            java.lang.Boolean.parseBoolean(
                                fileLoggerSDK
                            )
                        )
                    }
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
                    if (!legacyLoggingSettings.areKarereLogsEnabled() && cursor.getColumnIndex(
                            KEY_FILE_LOGGER_KARERE
                        ) != Constants.INVALID_VALUE
                    ) {
                        val fileLoggerKarere = decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    KEY_FILE_LOGGER_KARERE
                                )
                            )
                        )
                        legacyLoggingSettings.updateKarereLogs(
                            java.lang.Boolean.parseBoolean(
                                fileLoggerKarere
                            )
                        )
                    }
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
            null
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
            null
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
            null
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

    override fun setContactNickname(nickname: String?, handle: Long) {
        applicationScope.launch {
            megaLocalRoomGateway.updateContactNicknameByHandle(handle, nickname)
        }
    }

    override fun findContactByHandle(handleParam: Long): Contact? =
        runBlocking { megaLocalRoomGateway.getContactByHandle(handleParam) }

    override fun findContactByEmail(mail: String?): Contact? =
        runBlocking { megaLocalRoomGateway.getContactByEmail(mail) }

    override fun setOfflineFile(offline: MegaOffline): Long {
        Timber.d("setOfflineFile: %s", offline.handle)
        val values = ContentValues()
        val checkInsert: MegaOffline? = findByHandle(offline.handle)
        if (checkInsert == null) {
            values.put(KEY_OFF_HANDLE, encrypt(offline.handle))
            values.put(KEY_OFF_PATH, encrypt(offline.path))
            values.put(KEY_OFF_NAME, encrypt(offline.name))
            values.put(KEY_OFF_PARENT, offline.parentId)
            values.put(KEY_OFF_TYPE, encrypt(offline.type))
            values.put(KEY_OFF_INCOMING, offline.origin)
            values.put(KEY_OFF_HANDLE_INCOMING, encrypt(offline.handleIncoming))
            values.put(KEY_OFF_LAST_MODIFIED_TIME, System.currentTimeMillis())
            return writableDatabase.insert(TABLE_OFFLINE, SQLiteDatabase.CONFLICT_NONE, values)
        }
        return -1
    }

    override fun setOfflineFileOld(offline: MegaOffline): Long {
        val values = ContentValues()
        val checkInsert: MegaOffline? = findByHandle(offline.handle)
        if (checkInsert == null) {
            values.put(KEY_OFF_HANDLE, offline.handle)
            values.put(KEY_OFF_PATH, offline.path)
            values.put(KEY_OFF_NAME, offline.name)
            values.put(KEY_OFF_PARENT, offline.parentId)
            values.put(KEY_OFF_TYPE, offline.type)
            values.put(KEY_OFF_INCOMING, offline.origin)
            values.put(KEY_OFF_HANDLE_INCOMING, offline.handleIncoming)
            return writableDatabase.insert(TABLE_OFFLINE, SQLiteDatabase.CONFLICT_NONE, values)
        }
        return -1
    }

    override val offlineFiles: ArrayList<MegaOffline>
        get() {
            val listOffline = ArrayList<MegaOffline>()
            val selectQuery = "SELECT * FROM $TABLE_OFFLINE"
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        do {
                            val id = cursor.getString(0).toInt()
                            val handle = decrypt(cursor.getString(1))
                            val path = decrypt(cursor.getString(2))
                            val name = decrypt(cursor.getString(3))
                            val parent = cursor.getInt(4)
                            val type = decrypt(cursor.getString(5))
                            val incoming = cursor.getInt(6)
                            val handleIncoming = decrypt(cursor.getString(7))
                            val offline = MegaOffline(
                                id,
                                handle.toString(),
                                path.toString(),
                                name.toString(),
                                parent,
                                type,
                                incoming,
                                handleIncoming.toString()
                            )
                            listOffline.add(offline)
                        } while (cursor.moveToNext())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return listOffline
        }

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

    override fun findByParentId(parentId: Int): ArrayList<MegaOffline> {
        val listOffline = ArrayList<MegaOffline>()
        //Get the foreign key of the node
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_PARENT = '$parentId'"
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val _id = cursor.getString(0).toInt()
                        val _handle = decrypt(cursor.getString(1))
                        val _path = decrypt(cursor.getString(2))
                        val _name = decrypt(cursor.getString(3))
                        val _parent = cursor.getInt(4)
                        val _type = decrypt(cursor.getString(5))
                        val _incoming = cursor.getInt(6)
                        val _handleIncoming = decrypt(cursor.getString(7))
                        listOffline.add(
                            MegaOffline(
                                _id,
                                _handle.toString(),
                                _path.toString(),
                                _name.toString(),
                                _parent,
                                _type,
                                _incoming,
                                _handleIncoming.toString()
                            )
                        )
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return listOffline
    }

    override fun findById(id: Int): MegaOffline? {
        val selectQuery = "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_ID = '$id'"
        var offline: MegaOffline? = null
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val _id = cursor.getString(0).toInt()
                        val _handle = decrypt(cursor.getString(1))
                        val _path = decrypt(cursor.getString(2))
                        val _name = decrypt(cursor.getString(3))
                        val _parent = cursor.getInt(4)
                        val _type = decrypt(cursor.getString(5))
                        val _incoming = cursor.getInt(6)
                        val _handleIncoming = decrypt(cursor.getString(7))
                        offline = MegaOffline(
                            _id,
                            _handle.toString(),
                            _path.toString(),
                            _name.toString(),
                            _parent,
                            _type,
                            _incoming,
                            _handleIncoming.toString()
                        )
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return offline
    }

    override fun removeById(id: Int): Int {
        return writableDatabase.delete(TABLE_OFFLINE, "$KEY_ID=$id", null)
    }

    override fun findByPath(path: String?): ArrayList<MegaOffline> {
        val listOffline = ArrayList<MegaOffline>()
        //Get the foreign key of the node
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_PATH = '${encrypt(path)}'"
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val _id = cursor.getString(0).toInt()
                        val _handle = decrypt(cursor.getString(1))
                        val _path = decrypt(cursor.getString(2))
                        val _name = decrypt(cursor.getString(3))
                        val _parent = cursor.getInt(4)
                        val _type = decrypt(cursor.getString(5))
                        val _incoming = cursor.getInt(6)
                        val _handleIncoming = decrypt(cursor.getString(7))
                        listOffline.add(
                            MegaOffline(
                                _id,
                                _handle.toString(),
                                _path.toString(),
                                _name.toString(),
                                _parent,
                                _type,
                                _incoming,
                                _handleIncoming.toString()
                            )
                        )
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
            crashReporter.log("Exception loading offline node: ${e.message}")
        }
        return listOffline
    }

    override fun findbyPathAndName(path: String?, name: String?): MegaOffline? {
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_PATH = '${encrypt(path)}'AND $KEY_OFF_NAME = '${
                encrypt(name)
            }'"
        var offline: MegaOffline? = null
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val _id = cursor.getString(0).toInt()
                        val _handle = decrypt(cursor.getString(1))
                        val _path = decrypt(cursor.getString(2))
                        val _name = decrypt(cursor.getString(3))
                        val _parent = cursor.getInt(4)
                        val _type = decrypt(cursor.getString(5))
                        val _incoming = cursor.getInt(6)
                        val _handleIncoming = decrypt(cursor.getString(7))
                        offline = MegaOffline(
                            _id,
                            _handle.toString(),
                            _path.toString(),
                            _name.toString(),
                            _parent,
                            _type,
                            _incoming,
                            _handleIncoming.toString()
                        )
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return offline
    }

    override fun deleteOfflineFile(mOff: MegaOffline): Int {
        return writableDatabase.delete(
            TABLE_OFFLINE,
            "$KEY_OFF_HANDLE = ?",
            arrayOf(encrypt(mOff.handle.toString()))
        )
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

    override fun setCamSyncWifi(wifi: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_WIFI= '${encrypt(wifi.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_WIFI, encrypt(wifi.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setPreferredViewList(list: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_VIEW_LIST= '${encrypt(list.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_PREFERRED_VIEW_LIST, encrypt(list.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setPreferredViewListCamera(list: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_VIEW_LIST_CAMERA= '${
                            encrypt(list.toString())
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_PREFERRED_VIEW_LIST_CAMERA, encrypt(list.toString()))
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

    override fun setLastUploadFolder(folderPath: String) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_LAST_UPLOAD_FOLDER= '${
                            encrypt(folderPath)
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_LAST_UPLOAD_FOLDER, encrypt(folderPath))
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

    override fun setKeepFileNames(charging: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_KEEP_FILE_NAMES= '${encrypt(charging.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_KEEP_FILE_NAMES, encrypt(charging.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setCamSyncEnabled(enabled: Boolean) {
        Timber.d("setCamSyncEnabled: %s", enabled)
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_ENABLED= '${encrypt(enabled.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_ENABLED, encrypt(enabled.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setSecondaryUploadEnabled(enabled: Boolean) {
        Timber.d("setSecondaryUploadEnabled: %s", enabled)
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_ENABLED= '${encrypt(enabled.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_SEC_FOLDER_ENABLED, encrypt(enabled.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setCamSyncHandle(handle: Long) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_HANDLE= '${encrypt(handle.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_HANDLE, encrypt(handle.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
                Timber.d("Set new primary handle: %s", handle)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setSecondaryFolderHandle(handle: Long) {
        Timber.d("setSecondaryFolderHandle: %s", handle)
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_HANDLE= '${encrypt(handle.toString())}' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_SEC_FOLDER_HANDLE, encrypt(handle.toString()))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
                Timber.d("Set new secondary handle: %s", handle)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setCamSyncLocalPath(localPath: String) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_LOCAL_PATH= '${
                            encrypt(localPath)
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(localPath))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setUriExternalSDCard(uriExternalSDCard: String?) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_URI_EXTERNAL_SD_CARD= '${
                            encrypt(uriExternalSDCard)
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                    Timber.d("KEY_URI_EXTERNAL_SD_CARD URI: %s", UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_URI_EXTERNAL_SD_CARD, encrypt(uriExternalSDCard))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }
    /**
     * Gets the local path selected in an external SD card as Media Uploads local folder.
     *
     * @return The Media Uploads path located in SD card
     */
    /**
     * Sets the local path selected from an external SD card as Media Uploads local folder.
     *
     * @param uriMediaExternalSdCard local path
     */
    override var uriMediaExternalSdCard: String?
        get() = getStringValue(TABLE_PREFERENCES, KEY_URI_MEDIA_EXTERNAL_SD_CARD, "")
        set(uriMediaExternalSdCard) {
            setStringValue(
                TABLE_PREFERENCES,
                KEY_URI_MEDIA_EXTERNAL_SD_CARD,
                uriMediaExternalSdCard
            )
        }

    override fun setCameraFolderExternalSDCard(cameraFolderExternalSDCard: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD= '${
                            encrypt(cameraFolderExternalSDCard.toString())
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(
                        KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD,
                        encrypt(cameraFolderExternalSDCard.toString())
                    )
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }
    /**
     * Gets the flag which indicates if the local path selected as Media Uploads local folder belongs to an external SD card.
     *
     * @return True if the local path belongs to an external SD card, false otherwise
     */
    /**
     * Sets the flag to indicate if the local path selected as Media Uploads local folder belongs to an external SD card.
     *
     * @param mediaFolderExternalSdCard true if the local path selected belongs to an external SD card, false otherwise
     */
    override var mediaFolderExternalSdCard: Boolean
        get() = getBooleanValue(TABLE_PREFERENCES, KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD, false)
        set(mediaFolderExternalSdCard) {
            setStringValue(
                TABLE_PREFERENCES,
                KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD,
                mediaFolderExternalSdCard.toString()
            )
        }

    override var passcodeLockType: String?
        get() = getStringValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_TYPE, "")
        set(passcodeLockType) {
            Timber.d("setPasscodeLockType")
            val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
            val values = ContentValues()
            try {
                readableDatabase.query(selectQuery).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val UPDATE_PREFERENCES_TABLE =
                            "UPDATE $TABLE_PREFERENCES SET $KEY_PASSCODE_LOCK_TYPE= '${
                                encrypt(passcodeLockType)
                            }' WHERE $KEY_ID = '1'"
                        writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                    } else {
                        values.put(KEY_PASSCODE_LOCK_TYPE, encrypt(passcodeLockType))
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

    override fun setSecondaryFolderPath(localPath: String?) {
        Timber.d("setSecondaryFolderPath: %s", localPath)
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_LOCAL_PATH= '${
                            encrypt(localPath)
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_SEC_FOLDER_LOCAL_PATH, encrypt(localPath))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setCamSyncFileUpload(fileUpload: Int) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_FILE_UPLOAD= '${
                            encrypt(fileUpload.toString())
                        }' WHERE $KEY_ID = '1'"
                    writableDatabase.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(fileUpload.toString()))
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

    override fun setCamSyncTimeStamp(camSyncTimeStamp: Long) {
        Timber.d("setCamSyncTimeStamp: %s", camSyncTimeStamp)
        setLongValue(TABLE_PREFERENCES, KEY_CAM_SYNC_TIMESTAMP, camSyncTimeStamp)
    }

    override fun setCamVideoSyncTimeStamp(camVideoSyncTimeStamp: Long) {
        Timber.d("setCamVideoSyncTimeStamp: %s", camVideoSyncTimeStamp)
        setLongValue(TABLE_PREFERENCES, KEY_CAM_VIDEO_SYNC_TIMESTAMP, camVideoSyncTimeStamp)
    }

    override fun setSecSyncTimeStamp(secSyncTimeStamp: Long) {
        Timber.d("setSecSyncTimeStamp: %s", secSyncTimeStamp)
        setLongValue(TABLE_PREFERENCES, KEY_SEC_SYNC_TIMESTAMP, secSyncTimeStamp)
    }

    override fun setSecVideoSyncTimeStamp(secVideoSyncTimeStamp: Long) {
        Timber.d("setSecVideoSyncTimeStamp: %s", secVideoSyncTimeStamp)
        setLongValue(TABLE_PREFERENCES, KEY_SEC_VIDEO_SYNC_TIMESTAMP, secVideoSyncTimeStamp)
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
     * Gets the time required before ask for the passcode.
     *
     * @return The time required before ask for the passcode.
     */
    /**
     * Sets the time required before ask for the passcode.
     *
     * @param requiredTime The time required before ask for the passcode.
     */
    override var passcodeRequiredTime: Int
        get() = getStringValue(
            TABLE_PREFERENCES,
            KEY_PASSCODE_LOCK_REQUIRE_TIME,
            Constants.REQUIRE_PASSCODE_INVALID.toString()
        )?.toIntOrNull()
            ?: Constants.REQUIRE_PASSCODE_INVALID
        set(requiredTime) {
            setStringValue(
                TABLE_PREFERENCES,
                KEY_PASSCODE_LOCK_REQUIRE_TIME,
                requiredTime.toString()
            )
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

    override fun setAttrAskNoAppDownload(askNoAppDownload: String?) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_ASK_NOAPP_DOWNLOAD='${
                            encrypt(askNoAppDownload)
                        }' WHERE $KEY_ID ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_ATTR_ASK_NOAPP_DOWNLOAD, encrypt(askNoAppDownload))
                    writableDatabase.insert(TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setAttrAttempts(attempt: Int) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_INTENTS='${
                            encrypt(attempt.toString())
                        }' WHERE $KEY_ID ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_ATTR_INTENTS, encrypt(attempt.toString()))
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
     * Get the storage state value from the database.
     *
     * @return Storage state value.
     */
    /**
     * Set the storage state value into the database.
     *
     * @param storageState Storage state value.
     */
    override var storageState: StorageState
        get() {
            Timber.i("Getting the storage state from DB")
            return storageStateMapper(
                getIntValue(
                    tableName = TABLE_ATTRIBUTES,
                    columnName = KEY_STORAGE_STATE,
                    defaultValue = storageStateIntMapper(StorageState.Unknown),
                )
            )

        }
        set(storageState) {
            Timber.i("Setting the storage state in the DB")
            setIntValue(TABLE_ATTRIBUTES, KEY_STORAGE_STATE, storageStateIntMapper(storageState))
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
        legacyDatabaseMigration.onCreate(writableDatabase)
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

    override fun clearOffline() {
        writableDatabase.execSQL("DELETE TABLE IF EXISTS $TABLE_OFFLINE")
    }

    /**
     * Adds a pending message from File Explorer.
     *
     * @param message Pending message to add.
     * @return The identifier of the pending message.
     */
    override fun addPendingMessageFromFileExplorer(message: PendingMessage): Long {
        return addPendingMessage(message, PendingMessageState.PREPARING_FROM_EXPLORER.value)
    }

    /**
     * Adds a pending message.
     *
     * @param message Pending message to add.
     * @param state   State of the pending message.
     * @return The identifier of the pending message.
     */
    override fun addPendingMessage(message: PendingMessage): Long {
        return addPendingMessage(message, PendingMessageState.PREPARING.value)
    }

    /**
     * Adds a pending message.
     *
     * @param message Pending message to add.
     * @return The identifier of the pending message.
     */
    override fun addPendingMessage(
        message: PendingMessage,
        state: Int,
    ): Long {
        val values = ContentValues()
        values.put(KEY_PENDING_MSG_ID_CHAT, encrypt(message.chatId.toString()))
        values.put(KEY_PENDING_MSG_TIMESTAMP, encrypt(message.uploadTimestamp.toString()))
        values.put(KEY_PENDING_MSG_FILE_PATH, encrypt(message.filePath))
        values.put(KEY_PENDING_MSG_FINGERPRINT, encrypt(message.fingerprint))
        values.put(KEY_PENDING_MSG_NAME, encrypt(message.name))
        values.put(KEY_PENDING_MSG_TRANSFER_TAG, Constants.INVALID_ID)
        values.put(KEY_PENDING_MSG_STATE, state)
        return writableDatabase.insert(
            TABLE_PENDING_MSG_SINGLE,
            SQLiteDatabase.CONFLICT_NONE,
            values
        )
    }

    override fun findPendingMessageById(messageId: Long): PendingMessage? {
        Timber.d("findPendingMessageById")
        var pendMsg: PendingMessage? = null
        val selectQuery =
            "SELECT * FROM $TABLE_PENDING_MSG_SINGLE WHERE $KEY_ID ='$messageId'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val chatId = decrypt(cursor.getString(1))!!.toLong()
                    val timestamp = decrypt(cursor.getString(2))!!
                        .toLong()
                    val idKarereString = decrypt(cursor.getString(3))
                    var idTempKarere: Long = -1
                    if (idKarereString != null && !idKarereString.isEmpty()) {
                        idTempKarere = idKarereString.toLong()
                    }
                    val filePath = decrypt(cursor.getString(4))
                    val name = decrypt(cursor.getString(5))
                    val nodeHandleString = decrypt(cursor.getString(6))
                    var nodeHandle: Long = -1
                    if (!nodeHandleString.isNullOrEmpty()) {
                        nodeHandle = nodeHandleString.toLong()
                    }
                    val fingerPrint = decrypt(cursor.getString(7))
                    val transferTag = cursor.getInt(8)
                    val state = cursor.getInt(9)
                    pendMsg = PendingMessage(
                        messageId,
                        chatId,
                        timestamp,
                        idTempKarere,
                        filePath.orEmpty(),
                        fingerPrint,
                        name,
                        nodeHandle,
                        transferTag,
                        state
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return pendMsg
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
            null
        )
    }

    override fun updatePendingMessageOnAttach(idMessage: Long, temporalId: String?, state: Int) {
        val values = ContentValues()
        Timber.d("ID of my pending message to update: %s", temporalId)
        values.put(KEY_PENDING_MSG_TEMP_KARERE, encrypt(temporalId))
        values.put(KEY_PENDING_MSG_STATE, state)
        val where = "$KEY_ID=$idMessage"
        val rows =
            writableDatabase.update(
                TABLE_PENDING_MSG_SINGLE,
                SQLiteDatabase.CONFLICT_REPLACE,
                values,
                where,
                null
            )
        Timber.d("Rows updated: %s", rows)
    }

    override fun findPendingMessagesNotSent(idChat: Long): ArrayList<AndroidMegaChatMessage> {
        Timber.d("findPendingMessagesNotSent")
        val pendMsgs = ArrayList<AndroidMegaChatMessage>()
        val chat = idChat.toString()
        val selectQuery =
            "SELECT * FROM $TABLE_PENDING_MSG_SINGLE WHERE $KEY_PENDING_MSG_STATE < ${PendingMessageState.SENT.value} AND $KEY_ID_CHAT ='${
                encrypt(chat)
            }'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getLong(0)
                        val chatId = decrypt(cursor.getString(1))!!
                            .toLong()
                        val timestamp = decrypt(cursor.getString(2))!!
                            .toLong()
                        val idKarereString = decrypt(cursor.getString(3))
                        var idTempKarere: Long = -1
                        if (idKarereString != null && !idKarereString.isEmpty()) {
                            idTempKarere = idKarereString.toLong()
                        }
                        val filePath = decrypt(cursor.getString(4))
                        val name = decrypt(cursor.getString(5))
                        val nodeHandleString = decrypt(cursor.getString(6))
                        var nodeHandle: Long = -1
                        if (nodeHandleString != null && !nodeHandleString.isEmpty()) {
                            nodeHandle = nodeHandleString.toLong()
                        }
                        val fingerPrint = decrypt(cursor.getString(7))
                        val transferTag = cursor.getInt(8)
                        val state = cursor.getInt(9)
                        val pendMsg = PendingMessage(
                            id,
                            chatId,
                            timestamp,
                            idTempKarere,
                            filePath.orEmpty(),
                            fingerPrint,
                            name,
                            nodeHandle,
                            transferTag,
                            state
                        )
                        val aPMsg = AndroidMegaChatMessage(pendMsg, true)
                        pendMsgs.add(aPMsg)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        Timber.d("Found: %s", pendMsgs.size)
        return pendMsgs
    }

    override fun findPendingMessageByIdTempKarere(idTemp: Long): Long {
        Timber.d("findPendingMessageById: %s", idTemp)
        val idPend = "$idTemp"
        var id: Long = -1
        val selectQuery =
            "SELECT * FROM $TABLE_PENDING_MSG_SINGLE WHERE $KEY_PENDING_MSG_TEMP_KARERE = '${
                encrypt(idPend)
            }'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    id = cursor.getLong(0)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return id
    }

    override fun removeSentPendingMessages() {
        Timber.d("removeSentPendingMessages")
        writableDatabase.delete(
            TABLE_PENDING_MSG_SINGLE,
            KEY_PENDING_MSG_STATE + "=" + PendingMessageState.SENT.value,
            null
        )
    }

    override fun removePendingMessageByChatId(idChat: Long) {
        Timber.d("removePendingMessageByChatId")
        writableDatabase.delete(
            TABLE_PENDING_MSG_SINGLE,
            "$KEY_PENDING_MSG_ID_CHAT = '${encrypt(idChat.toString())}'",
            null
        )
    }

    override fun removePendingMessageById(idMsg: Long) {
        writableDatabase.delete(TABLE_PENDING_MSG_SINGLE, "$KEY_ID=$idMsg", null)
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

    override fun setShowInviteBanner(show: String) {
        Timber.d("setCloseInviteBanner")
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_SHOW_INVITE_BANNER='${encrypt(show)}' WHERE $KEY_ID ='1'"
                    writableDatabase.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_SHOW_INVITE_BANNER, encrypt(show))
                    writableDatabase.insert(TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override suspend fun getOfflineInformationList(
        nodePath: String,
        searchQuery: String?,
    ): List<Offline> {
        return if (!searchQuery.isNullOrEmpty()) {
            searchOfflineInformationByQuery(nodePath, searchQuery)
        } else {
            searchOfflineInformationByPath(nodePath)
        }
    }

    /**
     * Search [Offline] by path
     *
     * @param nodePath
     * @return list of [Offline]
     */
    private fun searchOfflineInformationByPath(nodePath: String): List<Offline> {
        val offlineList = mutableListOf<Offline>()
        //Get the foreign key of the node
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_PATH = '${encrypt(nodePath)}'"
        try {
            readableDatabase.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getString(0).toInt()
                        val handle = decrypt(cursor.getString(1))
                        val path = decrypt(cursor.getString(2))
                        val name = decrypt(cursor.getString(3))
                        val parent = cursor.getInt(4)
                        val type = decrypt(cursor.getString(5))
                        val incoming = cursor.getInt(6)
                        val handleIncoming = decrypt(cursor.getString(7))
                        offlineList.add(
                            Offline(
                                id,
                                handle.toString(),
                                path.toString(),
                                name.toString(),
                                parent,
                                type,
                                incoming,
                                handleIncoming.toString()
                            )
                        )
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return offlineList
    }

    /**
     * Search [Offline] by query
     *
     * @param path
     * @param searchQuery
     * @return list of [Offline]
     */
    private fun searchOfflineInformationByQuery(
        path: String,
        searchQuery: String,
    ): List<Offline> {
        val offlineList = mutableListOf<Offline>()
        val nodes = findByPath(path)
        for (node in nodes) {
            if (node.isFolder) {
                offlineList.addAll(
                    searchOfflineInformationByQuery(
                        getChildPath(node),
                        searchQuery
                    )
                )
            }

            if (node.name.lowercase(Locale.ROOT).contains(searchQuery.lowercase(Locale.ROOT)) &&
                FileUtil.isFileAvailable(
                    OfflineUtils.getOfflineFile(
                        MegaApplication.getInstance(),
                        node
                    )
                )
            ) {
                offlineList.add(
                    Offline(
                        node.id,
                        node.handle,
                        node.path,
                        node.name,
                        node.parentId,
                        node.type,
                        node.origin,
                        node.handleIncoming
                    )
                )
            }
        }
        return offlineList
    }

    /**
     * Get  path of the node
     * @param offline : [MegaOffline]
     */
    private fun getChildPath(offline: MegaOffline): String {
        return if (offline.path.endsWith(File.separator)) {
            offline.path + offline.name + File.separator
        } else {
            offline.path + File.separator + offline.name + File.separator
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
        const val TABLE_SYNC_RECORDS = "syncrecords"
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
        const val KEY_FILE_LOGGER_SDK = "filelogger"
        const val KEY_FILE_LOGGER_KARERE = "fileloggerkarere"
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

        //columns for table sync records
        const val KEY_SYNC_FILEPATH_ORI = "sync_filepath_origin"
        const val KEY_SYNC_FILEPATH_NEW = "sync_filepath_new"
        const val KEY_SYNC_FP_ORI = "sync_fingerprint_origin"
        const val KEY_SYNC_FP_NEW = "sync_fingerprint_new"
        const val KEY_SYNC_TIMESTAMP = "sync_timestamp"
        const val KEY_SYNC_STATE = "sync_state"
        const val KEY_SYNC_FILENAME = "sync_filename"
        const val KEY_SYNC_HANDLE = "sync_handle"
        const val KEY_SYNC_COPYONLY = "sync_copyonly"
        const val KEY_SYNC_SECONDARY = "sync_secondary"
        const val KEY_SYNC_TYPE = "sync_type"
        const val KEY_SYNC_LONGITUDE = "sync_longitude"
        const val KEY_SYNC_LATITUDE = "sync_latitude"
        const val CREATE_SYNC_RECORDS_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_SYNC_RECORDS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +
                    "$KEY_SYNC_FILEPATH_ORI TEXT," +
                    "$KEY_SYNC_FILEPATH_NEW TEXT," +
                    "$KEY_SYNC_FP_ORI TEXT," +
                    "$KEY_SYNC_FP_NEW TEXT," +
                    "$KEY_SYNC_TIMESTAMP TEXT," +
                    "$KEY_SYNC_FILENAME TEXT," +
                    "$KEY_SYNC_LONGITUDE TEXT," +
                    "$KEY_SYNC_LATITUDE TEXT," +
                    "$KEY_SYNC_STATE INTEGER," +
                    "$KEY_SYNC_TYPE INTEGER," +
                    "$KEY_SYNC_HANDLE TEXT," +
                    "$KEY_SYNC_COPYONLY BOOLEAN," +
                    "$KEY_SYNC_SECONDARY BOOLEAN)"
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
