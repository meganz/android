package mega.privacy.android.app

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import mega.privacy.android.data.database.LegacyDatabaseMigration
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.mapper.StorageStateIntMapper
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.ChatSettings
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import javax.inject.Inject

class LegacyDatabaseMigrationImpl @Inject constructor(
    private val storageStateMapper: StorageStateMapper,
    private val storageStateIntMapper: StorageStateIntMapper,
) : LegacyDatabaseMigration {
    override fun onCreate(db: SupportSQLiteDatabase) {
        Timber.d("onCreate")
        val CREATE_OFFLINE_TABLE =
            "CREATE TABLE IF NOT EXISTS ${MegaDatabaseConstant.TABLE_OFFLINE}(" +
                    "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                    "${SqliteDatabaseHandler.KEY_OFF_HANDLE} TEXT," +
                    "${SqliteDatabaseHandler.KEY_OFF_PATH} TEXT," +
                    "${SqliteDatabaseHandler.KEY_OFF_NAME} TEXT," +
                    "${SqliteDatabaseHandler.KEY_OFF_PARENT} INTEGER," +
                    "${SqliteDatabaseHandler.KEY_OFF_TYPE} INTEGER, " +
                    "${SqliteDatabaseHandler.KEY_OFF_INCOMING} INTEGER, " +
                    "${SqliteDatabaseHandler.KEY_OFF_HANDLE_INCOMING} INTEGER, " +
                    "${SqliteDatabaseHandler.KEY_OFF_LAST_MODIFIED_TIME} INTEGER )"
        db.execSQL(CREATE_OFFLINE_TABLE)

        val CREATE_PREFERENCES_TABLE =
            "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_PREFERENCES} (" +
                    "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY," +
                    "${SqliteDatabaseHandler.KEY_FIRST_LOGIN} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_CAM_SYNC_ENABLED} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_CAM_SYNC_HANDLE} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CAM_SYNC_LOCAL_PATH} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CAM_SYNC_WIFI} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_CAM_SYNC_FILE_UPLOAD} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_STORAGE_ASK_ALWAYS} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_STORAGE_DOWNLOAD_LOCATION} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_SEC_FOLDER_ENABLED} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_SEC_FOLDER_LOCAL_PATH} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_SEC_FOLDER_HANDLE} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_KEEP_FILE_NAMES} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_PREFERRED_VIEW_LIST} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_URI_EXTERNAL_SD_CARD} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_PREFERRED_SORT_CLOUD} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_PREFERRED_SORT_OTHERS} TEXT," +
                    "${SqliteDatabaseHandler.KEY_AUTO_PLAY} BOOLEAN," +
                    "${SqliteDatabaseHandler.KEY_UPLOAD_VIDEO_QUALITY} TEXT DEFAULT '${
                        SqliteDatabaseHandler.encrypt(
                            VideoQuality.ORIGINAL.value.toString()
                        )
                    }'," +
                    "${SqliteDatabaseHandler.KEY_CONVERSION_ON_CHARGING} BOOLEAN," +
                    "${SqliteDatabaseHandler.KEY_CHARGING_ON_SIZE} TEXT," +
                    "${SqliteDatabaseHandler.KEY_SHOULD_CLEAR_CAMSYNC_RECORDS} TEXT," +
                    "${SqliteDatabaseHandler.KEY_REMOVE_GPS} TEXT," +
                    "${SqliteDatabaseHandler.KEY_PREFERRED_SORT_CAMERA_UPLOAD} TEXT," +
                    "${SqliteDatabaseHandler.KEY_ASK_SET_DOWNLOAD_LOCATION} BOOLEAN" +
                    ")"
        db.execSQL(CREATE_PREFERENCES_TABLE)

        val CREATE_ATTRIBUTES_TABLE =
            "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_ATTRIBUTES}(" +
                    "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +                               //0
                    "${SqliteDatabaseHandler.KEY_ATTR_ONLINE} TEXT, " +                                     //1
                    "${SqliteDatabaseHandler.KEY_ATTR_INTENTS} TEXT, " +                                    //2
                    "${SqliteDatabaseHandler.KEY_ATTR_ASK_SIZE_DOWNLOAD} BOOLEAN, " +                       //3
                    "${SqliteDatabaseHandler.KEY_ATTR_ASK_NOAPP_DOWNLOAD} BOOLEAN, " +                      //4
                    "${SqliteDatabaseHandler.KEY_ACCOUNT_DETAILS_TIMESTAMP} TEXT, " +                       //5
                    "${SqliteDatabaseHandler.KEY_PAYMENT_METHODS_TIMESTAMP} TEXT, " +                       //6
                    "${SqliteDatabaseHandler.KEY_PRICING_TIMESTAMP} TEXT, " +                               //7
                    "${SqliteDatabaseHandler.KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP} TEXT, " +              //8
                    "${SqliteDatabaseHandler.KEY_INVALIDATE_SDK_CACHE} TEXT, " +                            //9
                    "${SqliteDatabaseHandler.KEY_USE_HTTPS_ONLY} TEXT, " +                                  //10
                    "${SqliteDatabaseHandler.KEY_SHOW_COPYRIGHT} TEXT, " +                                  //11
                    "${SqliteDatabaseHandler.KEY_SHOW_NOTIF_OFF} TEXT, " +                                  //12
                    "${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE} TEXT, " +                              //13
                    "${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TIMESTAMP} TEXT, " +                    //14
                    "${SqliteDatabaseHandler.KEY_STORAGE_STATE} INTEGER DEFAULT '${
                        SqliteDatabaseHandler.encrypt(
                            storageStateIntMapper(StorageState.Unknown).toString()
                        )
                    }'," +              //15
                    "${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TYPE} INTEGER DEFAULT '${
                        SqliteDatabaseHandler.encrypt(
                            MegaApiJava.AFFILIATE_TYPE_INVALID.toString()
                        )
                    }', " +  //16
                    "${SqliteDatabaseHandler.KEY_MY_CHAT_FILES_FOLDER_HANDLE} TEXT DEFAULT '${
                        SqliteDatabaseHandler.encrypt(
                            MegaApiJava.INVALID_HANDLE.toString()
                        )
                    }', " +         //17
                    "${SqliteDatabaseHandler.KEY_TRANSFER_QUEUE_STATUS} BOOLEAN DEFAULT '${
                        SqliteDatabaseHandler.encrypt(
                            "false"
                        )
                    }')"  //18 - True if the queue is paused, false otherwise
        db.execSQL(CREATE_ATTRIBUTES_TABLE)

        val CREATE_CONTACTS_TABLE =
            "CREATE TABLE IF NOT EXISTS ${MegaDatabaseConstant.TABLE_CONTACTS}(" +
                    "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                    "${SqliteDatabaseHandler.KEY_CONTACT_HANDLE} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CONTACT_MAIL} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CONTACT_NAME} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CONTACT_LAST_NAME} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CONTACT_NICKNAME} TEXT)"
        db.execSQL(CREATE_CONTACTS_TABLE)

        val CREATE_CHAT_ITEM_TABLE =
            "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_CHAT_ITEMS}(" +
                    "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_HANDLE} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_ITEM_NOTIFICATIONS} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_ITEM_RINGTONE} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_ITEM_SOUND_NOTIFICATIONS} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_ITEM_WRITTEN_TEXT} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_ITEM_EDITED_MSG_ID} TEXT)"
        db.execSQL(CREATE_CHAT_ITEM_TABLE)

        val CREATE_CHAT_TABLE =
            "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_CHAT_SETTINGS}(" +
                    "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_NOTIFICATIONS_ENABLED} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_SOUND_NOTIFICATIONS} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_VIBRATION_ENABLED} BOOLEAN, " +
                    "${SqliteDatabaseHandler.KEY_CHAT_VIDEO_QUALITY} TEXT DEFAULT '${
                        SqliteDatabaseHandler.encrypt(
                            VideoQuality.MEDIUM.value.toString()
                        )
                    }')"
        db.execSQL(CREATE_CHAT_TABLE)

        val CREATE_COMPLETED_TRANSFER_TABLE =
            "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS}(" +
                    "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +                      //0
                    "${SqliteDatabaseHandler.KEY_TRANSFER_FILENAME} TEXT, " +                      //1
                    "${SqliteDatabaseHandler.KEY_TRANSFER_TYPE} TEXT, " +                          //2
                    "${SqliteDatabaseHandler.KEY_TRANSFER_STATE} TEXT, " +                         //3
                    "${SqliteDatabaseHandler.KEY_TRANSFER_SIZE} TEXT, " +                          //4
                    "${SqliteDatabaseHandler.KEY_TRANSFER_HANDLE} TEXT, " +                        //5
                    "${SqliteDatabaseHandler.KEY_TRANSFER_PATH} TEXT, " +                          //6
                    "${SqliteDatabaseHandler.KEY_TRANSFER_OFFLINE} BOOLEAN, " +                    //7
                    "${SqliteDatabaseHandler.KEY_TRANSFER_TIMESTAMP} TEXT, " +                     //8
                    "${SqliteDatabaseHandler.KEY_TRANSFER_ERROR} TEXT, " +                         //9
                    "${SqliteDatabaseHandler.KEY_TRANSFER_ORIGINAL_PATH} TEXT, " +                 //10
                    "${SqliteDatabaseHandler.KEY_TRANSFER_PARENT_HANDLE} TEXT)"                    //11
        db.execSQL(CREATE_COMPLETED_TRANSFER_TABLE)

        val CREATE_EPHEMERAL =
            "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_EPHEMERAL}(" +
                    "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                    "${SqliteDatabaseHandler.KEY_EMAIL} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_PASSWORD} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_SESSION} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_FIRST_NAME} TEXT, " +
                    "${SqliteDatabaseHandler.KEY_LAST_NAME} TEXT)"
        db.execSQL(CREATE_EPHEMERAL)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Timber.i("Database upgraded from %d to %d", oldVersion, newVersion)

        //Used to identify when the Chat Settings table has been already recreated
        var chatSettingsAlreadyUpdated = false
        //Used to identify when the Attributes table has been already recreated
        var attributesAlreadyUpdated = false
        //Used to identify when the Preferences table has been already recreated
        var preferencesAlreadyUpdated = false
        if (oldVersion <= 7) {
            db.execSQL("ALTER TABLE ${MegaDatabaseConstant.TABLE_OFFLINE} ADD COLUMN ${SqliteDatabaseHandler.KEY_OFF_INCOMING} INTEGER;")
            db.execSQL("ALTER TABLE ${MegaDatabaseConstant.TABLE_OFFLINE} ADD COLUMN ${SqliteDatabaseHandler.KEY_OFF_HANDLE_INCOMING} INTEGER;")
            db.execSQL("UPDATE ${MegaDatabaseConstant.TABLE_OFFLINE} SET ${SqliteDatabaseHandler.KEY_OFF_INCOMING} = '0';")
        }
        if (oldVersion <= 12) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_SEC_FOLDER_ENABLED} TEXT;")
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_SEC_FOLDER_LOCAL_PATH} TEXT;")
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_SEC_FOLDER_HANDLE} TEXT;")
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_KEEP_FILE_NAMES} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_SEC_FOLDER_ENABLED} = '${
                    SqliteDatabaseHandler.encrypt(
                        "false"
                    )
                }';"
            )
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_SEC_FOLDER_LOCAL_PATH} = '${
                    SqliteDatabaseHandler.encrypt(
                        "-1"
                    )
                }';"
            )
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_SEC_FOLDER_HANDLE} = '${
                    SqliteDatabaseHandler.encrypt(
                        "-1"
                    )
                }';"
            )
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_KEEP_FILE_NAMES} = '${
                    SqliteDatabaseHandler.encrypt(
                        "false"
                    )
                }';"
            )
        }
        if (oldVersion <= 14) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_ATTR_INTENTS} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_ATTR_INTENTS} = '${
                    SqliteDatabaseHandler.encrypt(
                        "0"
                    )
                }';"
            )
        }
        if (oldVersion <= 15) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_PREFERRED_VIEW_LIST} BOOLEAN;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_PREFERRED_VIEW_LIST} = '${
                    SqliteDatabaseHandler.encrypt(
                        "true"
                    )
                }';"
            )
        }
        if (oldVersion <= 16) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_ATTR_ASK_SIZE_DOWNLOAD} BOOLEAN;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_ATTR_ASK_SIZE_DOWNLOAD} = '${
                    SqliteDatabaseHandler.encrypt(
                        "true"
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_ATTR_ASK_NOAPP_DOWNLOAD} BOOLEAN;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_ATTR_ASK_NOAPP_DOWNLOAD} = '${
                    SqliteDatabaseHandler.encrypt(
                        "true"
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_URI_EXTERNAL_SD_CARD} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_URI_EXTERNAL_SD_CARD} = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD} BOOLEAN;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD} = '${
                    SqliteDatabaseHandler.encrypt("false")
                }';"
            )
        }
        if (oldVersion <= 17) {
            val CREATE_CONTACTS_TABLE =
                "CREATE TABLE IF NOT EXISTS ${MegaDatabaseConstant.TABLE_CONTACTS}(" +
                        "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                        "${SqliteDatabaseHandler.KEY_CONTACT_HANDLE} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_CONTACT_MAIL} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_CONTACT_NAME} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_CONTACT_LAST_NAME} TEXT)"
            db.execSQL(CREATE_CONTACTS_TABLE)
        }
        if (oldVersion <= 18) {
            //Changes to encrypt the Offline table
            val offlinesOld = getOfflineFilesOld(db)
            Timber.d("Clear the table offline")
            this.clearOffline(db)
            for (i in offlinesOld.indices) {
                val offline = offlinesOld[i]
                if (offline.type == null || offline.type == "0" || offline.type == "1") {
                    Timber.d("Not encrypted: %s", offline.name)
                    this.setOfflineFile(offline, db) //using the method that encrypts
                } else {
                    Timber.d("Encrypted: %s", offline.name)
                    this.setOfflineFileOld(offline, db) //using the OLD method that doesn't encrypt
                }
            }
        }
        if (oldVersion <= 20) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_PREFERRED_SORT_CLOUD} TEXT;")
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_PREFERRED_SORT_OTHERS} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_PREFERRED_SORT_CLOUD} = '${
                    SqliteDatabaseHandler.encrypt(MegaApiJava.ORDER_DEFAULT_ASC.toString())
                }';"
            )
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_PREFERRED_SORT_OTHERS} = '${
                    SqliteDatabaseHandler.encrypt(MegaApiJava.ORDER_DEFAULT_ASC.toString())
                }';"
            )
        }
        if (oldVersion <= 21) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_ACCOUNT_DETAILS_TIMESTAMP} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_ACCOUNT_DETAILS_TIMESTAMP} = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_PAYMENT_METHODS_TIMESTAMP} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_PAYMENT_METHODS_TIMESTAMP} = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_PRICING_TIMESTAMP} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_PRICING_TIMESTAMP} = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP} = '${
                    SqliteDatabaseHandler.encrypt("")
                }';"
            )
        }
        if (oldVersion <= 22) {
            val CREATE_CHAT_ITEM_TABLE =
                "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_CHAT_ITEMS}(" +
                        "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                        "${SqliteDatabaseHandler.KEY_CHAT_HANDLE} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_CHAT_ITEM_NOTIFICATIONS} BOOLEAN, " +
                        "${SqliteDatabaseHandler.KEY_CHAT_ITEM_RINGTONE} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_CHAT_ITEM_SOUND_NOTIFICATIONS} TEXT)"
            db.execSQL(CREATE_CHAT_ITEM_TABLE)
            val CREATE_NONCONTACT_TABLE =
                "CREATE TABLE IF NOT EXISTS ${MegaDatabaseConstant.TABLE_NON_CONTACTS_LEGACY}(" +
                        "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                        "$KEY_NONCONTACT_HANDLE TEXT, " +
                        "$KEY_NONCONTACT_FULLNAME TEXT)"
            db.execSQL(CREATE_NONCONTACT_TABLE)
            val CREATE_CHAT_TABLE =
                "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_CHAT_SETTINGS}(" +
                        "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                        "${SqliteDatabaseHandler.KEY_CHAT_NOTIFICATIONS_ENABLED} BOOLEAN, " +
                        "${SqliteDatabaseHandler.KEY_CHAT_SOUND_NOTIFICATIONS} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_CHAT_VIBRATION_ENABLED} BOOLEAN)"
            db.execSQL(CREATE_CHAT_TABLE)
        }
        if (oldVersion <= 23) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_CREDENTIALS} ADD COLUMN ${SqliteDatabaseHandler.KEY_FIRST_NAME} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_CREDENTIALS} SET ${SqliteDatabaseHandler.KEY_FIRST_NAME} = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_CREDENTIALS} ADD COLUMN ${SqliteDatabaseHandler.KEY_LAST_NAME} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_CREDENTIALS} SET ${SqliteDatabaseHandler.KEY_LAST_NAME} = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
        }
        if (oldVersion <= 25) {
            db.execSQL("ALTER TABLE ${MegaDatabaseConstant.TABLE_NON_CONTACTS_LEGACY} ADD COLUMN $KEY_NONCONTACT_FIRSTNAME TEXT;")
            db.execSQL(
                "UPDATE ${MegaDatabaseConstant.TABLE_NON_CONTACTS_LEGACY} SET $KEY_NONCONTACT_FIRSTNAME = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${MegaDatabaseConstant.TABLE_NON_CONTACTS_LEGACY} ADD COLUMN $KEY_NONCONTACT_LASTNAME TEXT;")
            db.execSQL(
                "UPDATE ${MegaDatabaseConstant.TABLE_NON_CONTACTS_LEGACY} SET $KEY_NONCONTACT_LASTNAME = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
        }
        if (oldVersion <= 26) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_INVALIDATE_SDK_CACHE} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_INVALIDATE_SDK_CACHE} = '${
                    SqliteDatabaseHandler.encrypt(
                        "true"
                    )
                }';"
            )
        }
        if (oldVersion <= 27) {
            db.execSQL("ALTER TABLE ${MegaDatabaseConstant.TABLE_NON_CONTACTS_LEGACY} ADD COLUMN $KEY_NONCONTACT_EMAIL TEXT;")
            db.execSQL(
                "UPDATE ${MegaDatabaseConstant.TABLE_NON_CONTACTS_LEGACY} SET $KEY_NONCONTACT_EMAIL = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
        }
        if (oldVersion <= 28) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_CREDENTIALS} ADD COLUMN ${SqliteDatabaseHandler.KEY_MY_HANDLE} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_CREDENTIALS} SET ${SqliteDatabaseHandler.KEY_MY_HANDLE} = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
        }
        if (oldVersion <= 29) {
            val CREATE_COMPLETED_TRANSFER_TABLE =
                "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS}(" +
                        "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                        "${SqliteDatabaseHandler.KEY_TRANSFER_FILENAME} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_TRANSFER_TYPE} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_TRANSFER_STATE} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_TRANSFER_SIZE} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_TRANSFER_HANDLE} TEXT)"
            db.execSQL(CREATE_COMPLETED_TRANSFER_TABLE)
        }
        if (oldVersion <= 31) {
            val CREATE_EPHEMERAL =
                "CREATE TABLE IF NOT EXISTS ${SqliteDatabaseHandler.TABLE_EPHEMERAL}(" +
                        "${SqliteDatabaseHandler.KEY_ID} INTEGER PRIMARY KEY, " +
                        "${SqliteDatabaseHandler.KEY_EMAIL} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_PASSWORD} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_SESSION} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_FIRST_NAME} TEXT, " +
                        "${SqliteDatabaseHandler.KEY_LAST_NAME} TEXT)"
            db.execSQL(CREATE_EPHEMERAL)
        }
        if (oldVersion <= 34) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_USE_HTTPS_ONLY} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_USE_HTTPS_ONLY} = '${
                    SqliteDatabaseHandler.encrypt(
                        "false"
                    )
                }';"
            )
        }
        if (oldVersion <= 35) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_SHOW_COPYRIGHT} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_SHOW_COPYRIGHT} = '${
                    SqliteDatabaseHandler.encrypt(
                        "true"
                    )
                }';"
            )
        }
        if (oldVersion <= 37) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_CHAT_ITEMS} ADD COLUMN ${SqliteDatabaseHandler.KEY_CHAT_ITEM_WRITTEN_TEXT} TEXT;")
            db.execSQL("UPDATE ${SqliteDatabaseHandler.TABLE_CHAT_ITEMS} SET ${SqliteDatabaseHandler.KEY_CHAT_ITEM_WRITTEN_TEXT} = '';")
        }
        if (oldVersion <= 38) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_SHOW_NOTIF_OFF} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_SHOW_NOTIF_OFF} = '${
                    SqliteDatabaseHandler.encrypt(
                        "true"
                    )
                }';"
            )
        }
        if (oldVersion <= 41) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE} = '${
                    SqliteDatabaseHandler.encrypt(
                        "-1"
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TIMESTAMP} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TIMESTAMP} = '${
                    SqliteDatabaseHandler.encrypt("-1")
                }';"
            )
        }
        if (oldVersion <= 43) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_AUTO_PLAY} BOOLEAN;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_AUTO_PLAY} = '${
                    SqliteDatabaseHandler.encrypt(
                        "false"
                    )
                }';"
            )
        }
        if (oldVersion <= 44) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_UPLOAD_VIDEO_QUALITY} TEXT;")
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_CONVERSION_ON_CHARGING} BOOLEAN;")
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_CHARGING_ON_SIZE} TEXT;")
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_SHOULD_CLEAR_CAMSYNC_RECORDS} TEXT;")
        }
        if (oldVersion <= 45) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_REMOVE_GPS} TEXT;")
            db.execSQL(
                "UPDATE " + SqliteDatabaseHandler.TABLE_PREFERENCES + " SET " + SqliteDatabaseHandler.KEY_REMOVE_GPS + " = '" + SqliteDatabaseHandler.encrypt(
                    "true"
                ) + "';"
            )
        }
        if (oldVersion <= 46) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_STORAGE_STATE} INTEGER;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_STORAGE_STATE} = '${
                    SqliteDatabaseHandler.encrypt(storageStateIntMapper(StorageState.Unknown).toString())
                }';"
            )
        }
        if (oldVersion <= 48) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_PREFERRED_SORT_CAMERA_UPLOAD} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} " +
                        "SET ${SqliteDatabaseHandler.KEY_PREFERRED_SORT_CAMERA_UPLOAD} = " +
                        "'${SqliteDatabaseHandler.encrypt(MegaApiJava.ORDER_MODIFICATION_DESC.toString())}';"
            )
        }
        if (oldVersion <= 51) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TYPE} INTEGER;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} " +
                        "SET ${SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TYPE} = " +
                        "'${SqliteDatabaseHandler.encrypt(MegaApiJava.AFFILIATE_TYPE_INVALID.toString())}';"
            )
        }
        if (oldVersion <= 52) {
            recreateChatSettings(db, getChatSettingsFromDBv52(db))
            chatSettingsAlreadyUpdated = true
        }
        if (oldVersion <= 53) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_PREFERENCES} ADD COLUMN ${SqliteDatabaseHandler.KEY_ASK_SET_DOWNLOAD_LOCATION} BOOLEAN;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_ASK_SET_DOWNLOAD_LOCATION} = '${
                    SqliteDatabaseHandler.encrypt(
                        "true"
                    )
                }';"
            )
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_PREFERENCES} SET ${SqliteDatabaseHandler.KEY_STORAGE_ASK_ALWAYS} = '${
                    SqliteDatabaseHandler.encrypt(
                        "true"
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} ADD COLUMN ${SqliteDatabaseHandler.KEY_TRANSFER_PATH} TEXT;")
        }
        if (oldVersion <= 54) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_MY_CHAT_FILES_FOLDER_HANDLE} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} " +
                        "SET ${SqliteDatabaseHandler.KEY_MY_CHAT_FILES_FOLDER_HANDLE} = '${
                            SqliteDatabaseHandler.encrypt(
                                MegaApiJava.INVALID_HANDLE.toString()
                            )
                        }';"
            )
        }
        if (oldVersion <= 55) {
            db.execSQL("ALTER TABLE ${MegaDatabaseConstant.TABLE_CONTACTS} ADD COLUMN ${SqliteDatabaseHandler.KEY_CONTACT_NICKNAME} TEXT;")
        }
        if (oldVersion <= 56) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_CHAT_ITEMS} ADD COLUMN ${SqliteDatabaseHandler.KEY_CHAT_ITEM_EDITED_MSG_ID} TEXT;")
            db.execSQL("UPDATE ${SqliteDatabaseHandler.TABLE_CHAT_ITEMS} SET ${SqliteDatabaseHandler.KEY_CHAT_ITEM_EDITED_MSG_ID} = '';")
        }
        if (oldVersion <= 57) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} ADD COLUMN ${SqliteDatabaseHandler.KEY_TRANSFER_OFFLINE} BOOLEAN;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} SET ${SqliteDatabaseHandler.KEY_TRANSFER_OFFLINE} = '${
                    SqliteDatabaseHandler.encrypt(
                        "false"
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} ADD COLUMN ${SqliteDatabaseHandler.KEY_TRANSFER_TIMESTAMP} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} " +
                        "SET ${SqliteDatabaseHandler.KEY_TRANSFER_TIMESTAMP} = '${
                            SqliteDatabaseHandler.encrypt(System.currentTimeMillis().toString())
                        }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} ADD COLUMN ${SqliteDatabaseHandler.KEY_TRANSFER_ERROR} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} SET ${SqliteDatabaseHandler.KEY_TRANSFER_ERROR} = '${
                    SqliteDatabaseHandler.encrypt(
                        ""
                    )
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} ADD COLUMN ${SqliteDatabaseHandler.KEY_TRANSFER_ORIGINAL_PATH} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} SET ${SqliteDatabaseHandler.KEY_TRANSFER_ORIGINAL_PATH} = '${
                    SqliteDatabaseHandler.encrypt("")
                }';"
            )
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} ADD COLUMN ${SqliteDatabaseHandler.KEY_TRANSFER_PARENT_HANDLE} TEXT;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_COMPLETED_TRANSFERS} SET ${SqliteDatabaseHandler.KEY_TRANSFER_PARENT_HANDLE} = '${
                    SqliteDatabaseHandler.encrypt(MegaApiJava.INVALID_HANDLE.toString())
                }';"
            )
        }
        if (oldVersion <= 58) {
            db.execSQL("ALTER TABLE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} ADD COLUMN ${SqliteDatabaseHandler.KEY_TRANSFER_QUEUE_STATUS} BOOLEAN;")
            db.execSQL(
                "UPDATE ${SqliteDatabaseHandler.TABLE_ATTRIBUTES} SET ${SqliteDatabaseHandler.KEY_TRANSFER_QUEUE_STATUS} = '${
                    SqliteDatabaseHandler.encrypt(
                        "false"
                    )
                }';"
            )
        }
        if (oldVersion <= 61) {
            recreateAttributes(db, getAttributes(db))
            attributesAlreadyUpdated = true
        }
        if (oldVersion <= 62) {
            if (!chatSettingsAlreadyUpdated) {
                recreateChatSettings(db, getChatSettingsFromDBv62(db))
            }
            recreatePreferences(db, getPreferencesFromDBv62(db))
            preferencesAlreadyUpdated = true
        }
        if (oldVersion <= 64 && !preferencesAlreadyUpdated) {
            //KEY_CAM_SYNC_CHARGING and KEY_SMALL_GRID_CAMERA have been removed in DB v65
            recreatePreferences(db, getPreferences(db))
            preferencesAlreadyUpdated = true
        }
        if (oldVersion <= 65 && !preferencesAlreadyUpdated) {
            //KEY_PREFERRED_SORT_CONTACTS has been removed in DB v66
            recreatePreferences(db, getPreferences(db))
        }
        if (oldVersion <= 66 && !attributesAlreadyUpdated) {
            //KEY_FILE_LOGGER_SDK and KEY_FILE_LOGGER_KARERE have been removed in DB v67
            recreateAttributes(db, getAttributes(db))
        }
    }

    private fun getOfflineFilesOld(db: SupportSQLiteDatabase): ArrayList<MegaOffline> {
        val listOffline = ArrayList<MegaOffline>()
        val selectQuery = "SELECT * FROM ${MegaDatabaseConstant.TABLE_OFFLINE}"
        try {
            db.query(selectQuery)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getString(0).toInt()
                        val handle = cursor.getString(1)
                        val path = cursor.getString(2)
                        val name = cursor.getString(3)
                        val parent = cursor.getInt(4)
                        val type = cursor.getString(5)
                        val incoming = cursor.getInt(6)
                        val handleIncoming = cursor.getString(7)
                        val offline = MegaOffline(
                            id,
                            handle,
                            path,
                            name,
                            parent,
                            type,
                            incoming,
                            handleIncoming
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

    /**
     * Drops the preferences table if exists, creates the new one,
     * and then sets the updated preferences.
     *
     * @param db          Current DB.
     * @param preferences Preferences.
     */
    private fun recreatePreferences(db: SupportSQLiteDatabase, preferences: MegaPreferences?) {
        db.execSQL("DROP TABLE IF EXISTS ${SqliteDatabaseHandler.TABLE_PREFERENCES}")
        onCreate(db)
        preferences?.let { setPreferences(db, it) }

        // Temporary fix to avoid wrong values in preferences after upgrade.
        getPreferences(db)
    }

    /**
     * Drops the attributes table if exists, creates the new one,
     * and then sets the updated attributes.
     *
     * @param db   Current DB.
     * @param attr Attributes.
     */
    private fun recreateAttributes(db: SupportSQLiteDatabase, attr: MegaAttributes?) {
        db.execSQL("DROP TABLE IF EXISTS ${SqliteDatabaseHandler.TABLE_ATTRIBUTES}")
        onCreate(db)
        attr?.let { setAttributes(db, it) }

        // Temporary fix to avoid wrong values in attributes after upgrade.
        getAttributes(db)
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
        values.put(
            SqliteDatabaseHandler.KEY_ATTR_ONLINE,
            SqliteDatabaseHandler.encrypt(attr.online)
        )
        values.put(
            SqliteDatabaseHandler.KEY_ATTR_INTENTS,
            SqliteDatabaseHandler.encrypt(Integer.toString(attr.attempts))
        )
        values.put(
            SqliteDatabaseHandler.KEY_ATTR_ASK_SIZE_DOWNLOAD,
            SqliteDatabaseHandler.encrypt(attr.askSizeDownload)
        )
        values.put(
            SqliteDatabaseHandler.KEY_ATTR_ASK_NOAPP_DOWNLOAD,
            SqliteDatabaseHandler.encrypt(attr.askNoAppDownload)
        )
        values.put(
            SqliteDatabaseHandler.KEY_ACCOUNT_DETAILS_TIMESTAMP,
            SqliteDatabaseHandler.encrypt(attr.accountDetailsTimeStamp)
        )
        values.put(
            SqliteDatabaseHandler.KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP,
            SqliteDatabaseHandler.encrypt(attr.extendedAccountDetailsTimeStamp)
        )
        values.put(
            SqliteDatabaseHandler.KEY_INVALIDATE_SDK_CACHE,
            SqliteDatabaseHandler.encrypt(attr.invalidateSdkCache)
        )
        values.put(
            SqliteDatabaseHandler.KEY_USE_HTTPS_ONLY,
            SqliteDatabaseHandler.encrypt(attr.useHttpsOnly)
        )
        values.put(
            SqliteDatabaseHandler.KEY_USE_HTTPS_ONLY,
            SqliteDatabaseHandler.encrypt(attr.useHttpsOnly)
        )
        values.put(
            SqliteDatabaseHandler.KEY_SHOW_COPYRIGHT,
            SqliteDatabaseHandler.encrypt(attr.showCopyright)
        )
        values.put(
            SqliteDatabaseHandler.KEY_SHOW_NOTIF_OFF,
            SqliteDatabaseHandler.encrypt(attr.showNotifOff)
        )
        values.put(
            SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE,
            SqliteDatabaseHandler.encrypt(attr.lastPublicHandle.toString())
        )
        values.put(
            SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TIMESTAMP,
            SqliteDatabaseHandler.encrypt(attr.lastPublicHandleTimeStamp.toString())
        )
        values.put(
            SqliteDatabaseHandler.KEY_STORAGE_STATE,
            SqliteDatabaseHandler.encrypt(storageStateIntMapper(attr.storageState).toString())
        )
        values.put(
            SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TYPE,
            SqliteDatabaseHandler.encrypt(attr.lastPublicHandleType.toString())
        )
        values.put(
            SqliteDatabaseHandler.KEY_MY_CHAT_FILES_FOLDER_HANDLE,
            SqliteDatabaseHandler.encrypt(attr.myChatFilesFolderHandle.toString())
        )
        values.put(
            SqliteDatabaseHandler.KEY_TRANSFER_QUEUE_STATUS,
            SqliteDatabaseHandler.encrypt(attr.transferQueueStatus)
        )
        db.insert(SqliteDatabaseHandler.TABLE_ATTRIBUTES, SQLiteDatabase.CONFLICT_NONE, values)
    }

    /**
     * Gets attributes.
     *
     * @param db Current DB.
     * @return The attributes.
     */
    private fun getAttributes(db: SupportSQLiteDatabase): MegaAttributes? {
        var attr: MegaAttributes? = null
        val selectQuery = "SELECT * FROM ${SqliteDatabaseHandler.TABLE_ATTRIBUTES}"
        try {
            db.query(selectQuery)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val online = SqliteDatabaseHandler.decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                SqliteDatabaseHandler.KEY_ATTR_ONLINE
                            )
                        )
                    )
                    val intents =
                        SqliteDatabaseHandler.decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    SqliteDatabaseHandler.KEY_ATTR_INTENTS
                                )
                            )
                        )
                    val askSizeDownload = SqliteDatabaseHandler.decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                SqliteDatabaseHandler.KEY_ATTR_ASK_SIZE_DOWNLOAD
                            )
                        )
                    )
                    val askNoAppDownload = SqliteDatabaseHandler.decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                SqliteDatabaseHandler.KEY_ATTR_ASK_NOAPP_DOWNLOAD
                            )
                        )
                    )
                    val accountDetailsTimeStamp = SqliteDatabaseHandler.decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                SqliteDatabaseHandler.KEY_ACCOUNT_DETAILS_TIMESTAMP
                            )
                        )
                    )
                    val extendedAccountDetailsTimeStamp = SqliteDatabaseHandler.decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                SqliteDatabaseHandler.KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP
                            )
                        )
                    )
                    val invalidateSdkCache =
                        SqliteDatabaseHandler.decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    SqliteDatabaseHandler.KEY_INVALIDATE_SDK_CACHE
                                )
                            )
                        )
                    val useHttpsOnly =
                        SqliteDatabaseHandler.decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    SqliteDatabaseHandler.KEY_USE_HTTPS_ONLY
                                )
                            )
                        )
                    val showCopyright =
                        SqliteDatabaseHandler.decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    SqliteDatabaseHandler.KEY_SHOW_COPYRIGHT
                                )
                            )
                        )
                    val showNotifOff =
                        SqliteDatabaseHandler.decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    SqliteDatabaseHandler.KEY_SHOW_NOTIF_OFF
                                )
                            )
                        )
                    val lastPublicHandle =
                        SqliteDatabaseHandler.decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE
                                )
                            )
                        )
                    val lastPublicHandleTimeStamp = SqliteDatabaseHandler.decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TIMESTAMP
                            )
                        )
                    )
                    val storageState =
                        SqliteDatabaseHandler.decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    SqliteDatabaseHandler.KEY_STORAGE_STATE
                                )
                            )
                        )
                    val lastPublicHandleType = SqliteDatabaseHandler.decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                SqliteDatabaseHandler.KEY_LAST_PUBLIC_HANDLE_TYPE
                            )
                        )
                    )
                    val myChatFilesFolderHandle = SqliteDatabaseHandler.decrypt(
                        cursor.getString(
                            getColumnIndex(
                                cursor,
                                SqliteDatabaseHandler.KEY_MY_CHAT_FILES_FOLDER_HANDLE
                            )
                        )
                    )
                    val transferQueueStatus =
                        SqliteDatabaseHandler.decrypt(
                            cursor.getString(
                                getColumnIndex(
                                    cursor,
                                    SqliteDatabaseHandler.KEY_TRANSFER_QUEUE_STATUS
                                )
                            )
                        )
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


    private fun setOfflineFile(offline: MegaOffline, db: SupportSQLiteDatabase): Long {
        val values = ContentValues()
        val checkInsert: MegaOffline? = findByHandle(db, offline.handle)
        if (checkInsert == null) {
            values.put(
                SqliteDatabaseHandler.KEY_OFF_HANDLE,
                SqliteDatabaseHandler.encrypt(offline.handle)
            )
            values.put(
                SqliteDatabaseHandler.KEY_OFF_PATH,
                SqliteDatabaseHandler.encrypt(offline.path)
            )
            values.put(
                SqliteDatabaseHandler.KEY_OFF_NAME,
                SqliteDatabaseHandler.encrypt(offline.name)
            )
            values.put(SqliteDatabaseHandler.KEY_OFF_PARENT, offline.parentId)
            values.put(
                SqliteDatabaseHandler.KEY_OFF_TYPE,
                SqliteDatabaseHandler.encrypt(offline.type)
            )
            values.put(SqliteDatabaseHandler.KEY_OFF_INCOMING, offline.origin)
            values.put(
                SqliteDatabaseHandler.KEY_OFF_HANDLE_INCOMING,
                SqliteDatabaseHandler.encrypt(offline.handleIncoming)
            )
            return db.insert(
                MegaDatabaseConstant.TABLE_OFFLINE,
                SQLiteDatabase.CONFLICT_NONE,
                values
            )
        }
        return -1
    }


    private fun setOfflineFileOld(offline: MegaOffline, db: SupportSQLiteDatabase): Long {
        val values = ContentValues()
        val checkInsert: MegaOffline? = findByHandle(db, offline.handle)
        if (checkInsert == null) {
            values.put(SqliteDatabaseHandler.KEY_OFF_HANDLE, offline.handle)
            values.put(SqliteDatabaseHandler.KEY_OFF_PATH, offline.path)
            values.put(SqliteDatabaseHandler.KEY_OFF_NAME, offline.name)
            values.put(SqliteDatabaseHandler.KEY_OFF_PARENT, offline.parentId)
            values.put(SqliteDatabaseHandler.KEY_OFF_TYPE, offline.type)
            values.put(SqliteDatabaseHandler.KEY_OFF_INCOMING, offline.origin)
            values.put(SqliteDatabaseHandler.KEY_OFF_HANDLE_INCOMING, offline.handleIncoming)
            return db.insert(
                MegaDatabaseConstant.TABLE_OFFLINE,
                SQLiteDatabase.CONFLICT_NONE,
                values
            )
        }
        return -1
    }

    @Deprecated(
        message = "MegaOffline has been deprecated in favour of OfflineInformation",
        replaceWith = ReplaceWith("getOfflineInformation(handle)"),
        level = DeprecationLevel.WARNING
    )
    fun findByHandle(db: SupportSQLiteDatabase, handle: String?): MegaOffline? {
        //Get the foreign key of the node
        val selectQuery =
            "SELECT * FROM ${MegaDatabaseConstant.TABLE_OFFLINE} WHERE ${SqliteDatabaseHandler.KEY_OFF_HANDLE} = '${
                SqliteDatabaseHandler.encrypt(
                    handle
                )
            }'"
        try {
            db.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(0).toInt()
                    val nodeHandle = SqliteDatabaseHandler.decrypt(cursor.getString(1))
                    val path = SqliteDatabaseHandler.decrypt(cursor.getString(2))
                    val name = SqliteDatabaseHandler.decrypt(cursor.getString(3))
                    val parent = cursor.getInt(4)
                    val type = SqliteDatabaseHandler.decrypt(cursor.getString(5))
                    val incoming = cursor.getInt(6)
                    val handleIncoming = SqliteDatabaseHandler.decrypt(cursor.getString(7))
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

    private fun clearOffline(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS ${MegaDatabaseConstant.TABLE_OFFLINE}")
        onCreate(db)
    }

    /**
     * Drops the chat settings table if exists, creates the new one,
     * and then sets the updated chat settings.
     *
     * @param db           Current DB.
     * @param chatSettings Chat Settings.
     */
    private fun recreateChatSettings(db: SupportSQLiteDatabase, chatSettings: ChatSettings?) {
        db.execSQL("DROP TABLE IF EXISTS ${SqliteDatabaseHandler.TABLE_CHAT_SETTINGS}")
        onCreate(db)
        chatSettings?.let { setChatSettings(db, it) }

        // Temporary fix to avoid wrong values in chat settings after upgrade.
        getChatSettings(db)
    }

    /**
     * Get chat settings from the DB v52 (previous to remove the setting to enable/disable the chat).
     * KEY_CHAT_ENABLED and KEY_CHAT_STATUS have been removed in DB v53.
     *
     * @return Chat settings.
     */
    private fun getChatSettingsFromDBv52(db: SupportSQLiteDatabase): ChatSettings? {
        Timber.d("getChatSettings")
        var chatSettings: ChatSettings? = null
        val selectQuery = "SELECT * FROM ${SqliteDatabaseHandler.TABLE_CHAT_SETTINGS}"
        try {
            db.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    val notificationSound = SqliteDatabaseHandler.decrypt(cursor.getString(3))
                    val vibrationEnabled = SqliteDatabaseHandler.decrypt(cursor.getString(4))
                    val sendOriginalAttachments = SqliteDatabaseHandler.decrypt(cursor.getString(6))
                    val videoQuality =
                        if (sendOriginalAttachments.toBoolean()) VideoQuality.ORIGINAL.value.toString() else VideoQuality.MEDIUM.value.toString()
                    chatSettings =
                        ChatSettings(
                            notificationSound.orEmpty(),
                            vibrationEnabled ?: ChatSettings.VIBRATION_ON,
                            videoQuality
                        )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return chatSettings
    }

    /**
     * Get chat settings from the DB v62 (previous to remove the setting to enable/disable
     * the send original attachments and to add four available video qualities).
     * KEY_CHAT_SEND_ORIGINALS has been removed in DB v63.
     *
     * @return Chat settings.
     */
    private fun getChatSettingsFromDBv62(db: SupportSQLiteDatabase): ChatSettings? {
        Timber.d("getChatSettings")
        var chatSettings: ChatSettings? = null
        val selectQuery = "SELECT * FROM ${SqliteDatabaseHandler.TABLE_CHAT_SETTINGS}"
        try {
            db.query(selectQuery)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val notificationSound = SqliteDatabaseHandler.decrypt(cursor.getString(2))
                    val vibrationEnabled = SqliteDatabaseHandler.decrypt(cursor.getString(3))
                    val sendOriginalAttachments = SqliteDatabaseHandler.decrypt(cursor.getString(4))
                    val videoQuality =
                        if (sendOriginalAttachments.toBoolean()) VideoQuality.ORIGINAL.value.toString() else VideoQuality.MEDIUM.value.toString()
                    chatSettings =
                        ChatSettings(
                            notificationSound.orEmpty(),
                            vibrationEnabled ?: ChatSettings.VIBRATION_ON,
                            videoQuality
                        )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return chatSettings
    }

    /**
     * Gets preferences from the DB v62 (previous to add four available video qualities).
     *
     * @param db Current DB.
     * @return Preferences.
     */
    private fun getPreferencesFromDBv62(db: SupportSQLiteDatabase): MegaPreferences? {
        Timber.d("getPreferencesFromDBv62")

        return getPreferences(db)?.also { pref ->
            val uploadVideoQuality = pref.uploadVideoQuality
            if (!uploadVideoQuality.isNullOrBlank()
                && uploadVideoQuality.toInt() == SqliteDatabaseHandler.OLD_VIDEO_QUALITY_ORIGINAL
            ) {
                pref.uploadVideoQuality = VideoQuality.ORIGINAL.value.toString()
            }
        }
    }


    /**
     * Gets preferences.
     *
     * @param db Current DB.
     * @return Preferences.
     */
    private fun getPreferences(db: SupportSQLiteDatabase): MegaPreferences? {
        var prefs: MegaPreferences? = null
        val selectQuery = "SELECT * FROM ${SqliteDatabaseHandler.TABLE_PREFERENCES}"
        try {
            db.query(selectQuery).use { cursor ->
                if (cursor.moveToFirst()) {
                    fun column(key: String): String? =
                        SqliteDatabaseHandler.decrypt(cursor.getString(getColumnIndex(cursor, key)))

                    prefs = MegaPreferences(
                        column(SqliteDatabaseHandler.KEY_FIRST_LOGIN),
                        column(SqliteDatabaseHandler.KEY_CAM_SYNC_WIFI),
                        column(SqliteDatabaseHandler.KEY_CAM_SYNC_ENABLED),
                        column(SqliteDatabaseHandler.KEY_CAM_SYNC_HANDLE),
                        column(SqliteDatabaseHandler.KEY_CAM_SYNC_LOCAL_PATH),
                        column(SqliteDatabaseHandler.KEY_CAM_SYNC_FILE_UPLOAD),
                        column(SqliteDatabaseHandler.KEY_STORAGE_ASK_ALWAYS),
                        column(SqliteDatabaseHandler.KEY_STORAGE_DOWNLOAD_LOCATION),
                        column(SqliteDatabaseHandler.KEY_SEC_FOLDER_ENABLED),
                        column(SqliteDatabaseHandler.KEY_SEC_FOLDER_LOCAL_PATH),
                        column(SqliteDatabaseHandler.KEY_SEC_FOLDER_HANDLE),
                        column(SqliteDatabaseHandler.KEY_KEEP_FILE_NAMES),
                        column(SqliteDatabaseHandler.KEY_PREFERRED_VIEW_LIST),
                        column(SqliteDatabaseHandler.KEY_URI_EXTERNAL_SD_CARD),
                        column(SqliteDatabaseHandler.KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD),
                        column(SqliteDatabaseHandler.KEY_PREFERRED_SORT_CLOUD),
                        column(SqliteDatabaseHandler.KEY_PREFERRED_SORT_OTHERS),
                        column(SqliteDatabaseHandler.KEY_UPLOAD_VIDEO_QUALITY),
                        column(SqliteDatabaseHandler.KEY_CONVERSION_ON_CHARGING),
                        column(SqliteDatabaseHandler.KEY_CHARGING_ON_SIZE),
                        column(SqliteDatabaseHandler.KEY_AUTO_PLAY),
                        column(SqliteDatabaseHandler.KEY_REMOVE_GPS),
                        column(SqliteDatabaseHandler.KEY_PREFERRED_SORT_CAMERA_UPLOAD),
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return prefs
    }

    /**
     * Sets preferences.
     *
     * @param db    Current DB.
     * @param prefs Preferences.
     */
    private fun setPreferences(db: SupportSQLiteDatabase, prefs: MegaPreferences) {
        val values = ContentValues().apply {
            put(
                SqliteDatabaseHandler.KEY_FIRST_LOGIN,
                SqliteDatabaseHandler.encrypt(prefs.getFirstTime())
            )
            put(
                SqliteDatabaseHandler.KEY_CAM_SYNC_WIFI,
                SqliteDatabaseHandler.encrypt(prefs.getCamSyncWifi())
            )
            put(
                SqliteDatabaseHandler.KEY_CAM_SYNC_ENABLED,
                SqliteDatabaseHandler.encrypt(prefs.getCamSyncEnabled())
            )
            put(
                SqliteDatabaseHandler.KEY_CAM_SYNC_HANDLE,
                SqliteDatabaseHandler.encrypt(prefs.getCamSyncHandle())
            )
            put(
                SqliteDatabaseHandler.KEY_CAM_SYNC_LOCAL_PATH,
                SqliteDatabaseHandler.encrypt(prefs.getCamSyncLocalPath())
            )
            put(
                SqliteDatabaseHandler.KEY_CAM_SYNC_FILE_UPLOAD,
                SqliteDatabaseHandler.encrypt(prefs.getCamSyncFileUpload())
            )
            put(
                SqliteDatabaseHandler.KEY_STORAGE_ASK_ALWAYS,
                SqliteDatabaseHandler.encrypt(prefs.getStorageAskAlways())
            )
            put(
                SqliteDatabaseHandler.KEY_STORAGE_DOWNLOAD_LOCATION,
                SqliteDatabaseHandler.encrypt(prefs.getStorageDownloadLocation())
            )
            put(
                SqliteDatabaseHandler.KEY_SEC_FOLDER_ENABLED,
                SqliteDatabaseHandler.encrypt(prefs.getSecondaryMediaFolderEnabled())
            )
            put(
                SqliteDatabaseHandler.KEY_SEC_FOLDER_LOCAL_PATH,
                SqliteDatabaseHandler.encrypt(prefs.getLocalPathSecondaryFolder())
            )
            put(
                SqliteDatabaseHandler.KEY_SEC_FOLDER_HANDLE,
                SqliteDatabaseHandler.encrypt(prefs.getMegaHandleSecondaryFolder())
            )
            put(
                SqliteDatabaseHandler.KEY_PREFERRED_VIEW_LIST,
                SqliteDatabaseHandler.encrypt(prefs.getPreferredViewList())
            )
            put(
                SqliteDatabaseHandler.KEY_URI_EXTERNAL_SD_CARD,
                SqliteDatabaseHandler.encrypt(prefs.getUriExternalSDCard())
            )
            put(
                SqliteDatabaseHandler.KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD,
                SqliteDatabaseHandler.encrypt(prefs.getCameraFolderExternalSDCard())
            )
            put(
                SqliteDatabaseHandler.KEY_PREFERRED_SORT_CLOUD,
                SqliteDatabaseHandler.encrypt(prefs.getPreferredSortCloud())
            )
            put(
                SqliteDatabaseHandler.KEY_PREFERRED_SORT_CAMERA_UPLOAD,
                SqliteDatabaseHandler.encrypt(prefs.getPreferredSortCameraUpload())
            )
            put(
                SqliteDatabaseHandler.KEY_PREFERRED_SORT_OTHERS,
                SqliteDatabaseHandler.encrypt(prefs.getPreferredSortOthers())
            )
            put(
                SqliteDatabaseHandler.KEY_REMOVE_GPS,
                SqliteDatabaseHandler.encrypt(prefs.getRemoveGPS())
            )
            put(
                SqliteDatabaseHandler.KEY_KEEP_FILE_NAMES,
                SqliteDatabaseHandler.encrypt(prefs.getKeepFileNames())
            )
            put(
                SqliteDatabaseHandler.KEY_AUTO_PLAY,
                SqliteDatabaseHandler.encrypt(prefs.getIsAutoPlayEnabled())
            )
            put(
                SqliteDatabaseHandler.KEY_UPLOAD_VIDEO_QUALITY,
                SqliteDatabaseHandler.encrypt(prefs.getUploadVideoQuality())
            )
            put(
                SqliteDatabaseHandler.KEY_CONVERSION_ON_CHARGING,
                SqliteDatabaseHandler.encrypt(prefs.getConversionOnCharging())
            )
            put(
                SqliteDatabaseHandler.KEY_CHARGING_ON_SIZE,
                SqliteDatabaseHandler.encrypt(prefs.getChargingOnSize())
            )
        }

        db.insert(SqliteDatabaseHandler.TABLE_PREFERENCES, SQLiteDatabase.CONFLICT_NONE, values)
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
        db.execSQL("DELETE FROM ${SqliteDatabaseHandler.TABLE_CHAT_SETTINGS}")
        val values = ContentValues().apply {
            put(SqliteDatabaseHandler.KEY_CHAT_NOTIFICATIONS_ENABLED, "")
            put(
                SqliteDatabaseHandler.KEY_CHAT_SOUND_NOTIFICATIONS,
                SqliteDatabaseHandler.encrypt(chatSettings.notificationsSound)
            )
            put(
                SqliteDatabaseHandler.KEY_CHAT_VIBRATION_ENABLED,
                SqliteDatabaseHandler.encrypt(chatSettings.vibrationEnabled)
            )
            put(
                SqliteDatabaseHandler.KEY_CHAT_VIDEO_QUALITY,
                SqliteDatabaseHandler.encrypt(chatSettings.videoQuality)
            )
        }

        db.insert(SqliteDatabaseHandler.TABLE_CHAT_SETTINGS, SQLiteDatabase.CONFLICT_NONE, values)
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
        val selectQuery = "SELECT * FROM ${SqliteDatabaseHandler.TABLE_CHAT_SETTINGS}"
        try {
            db.query(selectQuery)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val notificationSound = SqliteDatabaseHandler.decrypt(cursor.getString(2))
                    val vibrationEnabled = SqliteDatabaseHandler.decrypt(cursor.getString(3))
                    val videoQuality = SqliteDatabaseHandler.decrypt(cursor.getString(4))
                    chatSettings =
                        ChatSettings(
                            notificationSound.orEmpty(),
                            vibrationEnabled ?: ChatSettings.VIBRATION_ON,
                            videoQuality ?: VideoQuality.MEDIUM.value.toString()
                        )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return chatSettings
    }

    private companion object {
        const val KEY_NONCONTACT_HANDLE = "noncontacthandle"
        const val KEY_NONCONTACT_FULLNAME = "noncontactfullname"
        const val KEY_NONCONTACT_FIRSTNAME = "noncontactfirstname"
        const val KEY_NONCONTACT_LASTNAME = "noncontactlastname"
        const val KEY_NONCONTACT_EMAIL = "noncontactemail"
    }
}
