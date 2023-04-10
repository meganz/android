package mega.privacy.android.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import dagger.hilt.android.EntryPointAccessors.fromApplication
import mega.privacy.android.app.di.LegacyLoggingEntryPoint
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage
import mega.privacy.android.app.main.megachat.ChatItemPreferences
import mega.privacy.android.app.main.megachat.PendingMessageSingle
import mega.privacy.android.app.objects.SDTransfer
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.removePrimaryBackup
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.removeSecondaryBackup
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.setPrimaryBackup
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.setSecondaryBackup
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.updatePrimaryFolderTargetNode
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.updateSecondaryFolderTargetNode
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.PasscodeUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.contacts.MegaContactGetter.MegaContact
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.database.DatabaseHandler.Companion.MAX_TRANSFERS
import mega.privacy.android.data.mapper.StorageStateIntMapper
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.data.model.ChatSettings
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaContactDB
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.user.UserCredentials
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.Locale

/**
 * Sqlite implementation of database handler
 */
class SqliteDatabaseHandler(
    context: Context?,
    private val legacyLoggingSettings: LegacyLoggingSettings,
    private val storageStateMapper: StorageStateMapper,
    private val storageStateIntMapper: StorageStateIntMapper,
) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), LegacyDatabaseHandler {
    private var db: SQLiteDatabase
    override fun onCreate(db: SQLiteDatabase) {
        Timber.d("onCreate")
        val CREATE_OFFLINE_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_OFFLINE(" +
                "$KEY_ID INTEGER PRIMARY KEY, " +
                "$KEY_OFF_HANDLE TEXT," +
                "$KEY_OFF_PATH TEXT," +
                "$KEY_OFF_NAME TEXT," +
                "$KEY_OFF_PARENT INTEGER," +
                "$KEY_OFF_TYPE INTEGER, " +
                "$KEY_OFF_INCOMING INTEGER, " +
                "$KEY_OFF_HANDLE_INCOMING INTEGER )"
        db.execSQL(CREATE_OFFLINE_TABLE)

        val CREATE_CREDENTIALS_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_CREDENTIALS(" +
                    "$KEY_ID INTEGER PRIMARY KEY," +
                    "$KEY_EMAIL TEXT, " +
                    "$KEY_SESSION TEXT, " +
                    "$KEY_FIRST_NAME TEXT, " +
                    "$KEY_LAST_NAME TEXT, " +
                    "$KEY_MY_HANDLE TEXT)"
        db.execSQL(CREATE_CREDENTIALS_TABLE)

        val CREATE_PREFERENCES_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_PREFERENCES (" +
                "$KEY_ID INTEGER PRIMARY KEY," +                    //0
                "$KEY_FIRST_LOGIN BOOLEAN, " +                      //1
                "$KEY_CAM_SYNC_ENABLED BOOLEAN, " +                 //2
                "$KEY_CAM_SYNC_HANDLE TEXT, " +                     //3
                "$KEY_CAM_SYNC_LOCAL_PATH TEXT, " +                 //4
                "$KEY_CAM_SYNC_WIFI BOOLEAN, " +                    //5
                "$KEY_CAM_SYNC_FILE_UPLOAD TEXT, " +                //6
                "$KEY_PASSCODE_LOCK_ENABLED TEXT, " +               //7
                "$KEY_PASSCODE_LOCK_CODE TEXT, " +                  //8
                "$KEY_STORAGE_ASK_ALWAYS TEXT, " +                  //9
                "$KEY_STORAGE_DOWNLOAD_LOCATION TEXT, " +           //10
                "$KEY_CAM_SYNC_TIMESTAMP TEXT, " +                  //11
                "$KEY_LAST_UPLOAD_FOLDER TEXT, " +                  //12
                "$KEY_LAST_CLOUD_FOLDER_HANDLE TEXT, " +            //13
                "$KEY_SEC_FOLDER_ENABLED TEXT, " +                  //14
                "$KEY_SEC_FOLDER_LOCAL_PATH TEXT, " +               //15
                "$KEY_SEC_FOLDER_HANDLE TEXT, " +                   //16
                "$KEY_SEC_SYNC_TIMESTAMP TEXT, " +                  //17
                "$KEY_KEEP_FILE_NAMES BOOLEAN, " +                  //18
                "$KEY_STORAGE_ADVANCED_DEVICES BOOLEAN, " +         //19
                "$KEY_PREFERRED_VIEW_LIST BOOLEAN, " +              //20
                "$KEY_PREFERRED_VIEW_LIST_CAMERA BOOLEAN, " +       //21
                "$KEY_URI_EXTERNAL_SD_CARD TEXT, " +                //22
                "$KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD BOOLEAN, " +   //23
                "$KEY_PASSCODE_LOCK_TYPE TEXT, " +                  //24
                "$KEY_PREFERRED_SORT_CLOUD TEXT, " +                //25
                "$KEY_PREFERRED_SORT_OTHERS TEXT," +                //26
                "$KEY_FIRST_LOGIN_CHAT BOOLEAN, " +                 //27
                "$KEY_AUTO_PLAY BOOLEAN," +                         //28
                "$KEY_UPLOAD_VIDEO_QUALITY TEXT DEFAULT '${encrypt(VideoQuality.ORIGINAL.value.toString())}'," +  //29
                "$KEY_CONVERSION_ON_CHARGING BOOLEAN," +            //30
                "$KEY_CHARGING_ON_SIZE TEXT," +                     //31
                "$KEY_SHOULD_CLEAR_CAMSYNC_RECORDS TEXT," +         //32
                "$KEY_CAM_VIDEO_SYNC_TIMESTAMP TEXT," +             //33
                "$KEY_SEC_VIDEO_SYNC_TIMESTAMP TEXT," +             //34
                "$KEY_REMOVE_GPS TEXT," +                           //35
                "$KEY_SHOW_INVITE_BANNER TEXT," +                   //36
                "$KEY_PREFERRED_SORT_CAMERA_UPLOAD TEXT," +         //37
                "$KEY_SD_CARD_URI TEXT," +                          //38
                "$KEY_ASK_FOR_DISPLAY_OVER TEXT," +                 //39
                "$KEY_ASK_SET_DOWNLOAD_LOCATION BOOLEAN," +         //40
                "$KEY_URI_MEDIA_EXTERNAL_SD_CARD TEXT," +           //41
                "$KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD BOOLEAN," +     //42
                "$KEY_PASSCODE_LOCK_REQUIRE_TIME TEXT DEFAULT '${encrypt(Constants.REQUIRE_PASSCODE_INVALID.toString())}', " + //43
                "$KEY_FINGERPRINT_LOCK BOOLEAN DEFAULT '" + encrypt("false") + "'" + //44
                ")"
        db.execSQL(CREATE_PREFERENCES_TABLE)

        val CREATE_ATTRIBUTES_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_ATTRIBUTES(" +
                "$KEY_ID INTEGER PRIMARY KEY, " +                               //0
                "$KEY_ATTR_ONLINE TEXT, " +                                     //1
                "$KEY_ATTR_INTENTS TEXT, " +                                    //2
                "$KEY_ATTR_ASK_SIZE_DOWNLOAD BOOLEAN, " +                       //3
                "$KEY_ATTR_ASK_NOAPP_DOWNLOAD BOOLEAN, " +                      //4
                "$KEY_ACCOUNT_DETAILS_TIMESTAMP TEXT, " +                       //5
                "$KEY_PAYMENT_METHODS_TIMESTAMP TEXT, " +                       //6
                "$KEY_PRICING_TIMESTAMP TEXT, " +                               //7
                "$KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP TEXT, " +              //8
                "$KEY_INVALIDATE_SDK_CACHE TEXT, " +                            //9
                "$KEY_USE_HTTPS_ONLY TEXT, " +                                  //10
                "$KEY_SHOW_COPYRIGHT TEXT, " +                                  //11
                "$KEY_SHOW_NOTIF_OFF TEXT, " +                                  //12
                "$KEY_LAST_PUBLIC_HANDLE TEXT, " +                              //13
                "$KEY_LAST_PUBLIC_HANDLE_TIMESTAMP TEXT, " +                    //14
                "$KEY_STORAGE_STATE INTEGER DEFAULT '${encrypt(storageStateIntMapper(StorageState.Unknown).toString())}'," +              //15
                "$KEY_LAST_PUBLIC_HANDLE_TYPE INTEGER DEFAULT '${encrypt(MegaApiJava.AFFILIATE_TYPE_INVALID.toString())}', " +  //16
                "$KEY_MY_CHAT_FILES_FOLDER_HANDLE TEXT DEFAULT '${encrypt(MegaApiJava.INVALID_HANDLE.toString())}', " +         //17
                "$KEY_TRANSFER_QUEUE_STATUS BOOLEAN DEFAULT '${encrypt("false")}')"  //18 - True if the queue is paused, false otherwise
        db.execSQL(CREATE_ATTRIBUTES_TABLE)

        val CREATE_CONTACTS_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_CONTACTS(" +
                "$KEY_ID INTEGER PRIMARY KEY, " +
                "$KEY_CONTACT_HANDLE TEXT, " +
                "$KEY_CONTACT_MAIL TEXT, " +
                "$KEY_CONTACT_NAME TEXT, " +
                "$KEY_CONTACT_LAST_NAME TEXT, " +
                "$KEY_CONTACT_NICKNAME TEXT)"
        db.execSQL(CREATE_CONTACTS_TABLE)

        val CREATE_CHAT_ITEM_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_CHAT_ITEMS(" +
                "$KEY_ID INTEGER PRIMARY KEY, " +
                "$KEY_CHAT_HANDLE TEXT, " +
                "$KEY_CHAT_ITEM_NOTIFICATIONS BOOLEAN, " +
                "$KEY_CHAT_ITEM_RINGTONE TEXT, " +
                "$KEY_CHAT_ITEM_SOUND_NOTIFICATIONS TEXT, " +
                "$KEY_CHAT_ITEM_WRITTEN_TEXT TEXT, " +
                "$KEY_CHAT_ITEM_EDITED_MSG_ID TEXT)"
        db.execSQL(CREATE_CHAT_ITEM_TABLE)

        val CREATE_NONCONTACT_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_NON_CONTACTS(" +
                "$KEY_ID INTEGER PRIMARY KEY, " +
                "$KEY_NONCONTACT_HANDLE TEXT, " +
                "$KEY_NONCONTACT_FULLNAME TEXT, " +
                "$KEY_NONCONTACT_FIRSTNAME TEXT, " +
                "$KEY_NONCONTACT_LASTNAME TEXT, " +
                "$KEY_NONCONTACT_EMAIL TEXT)"
        db.execSQL(CREATE_NONCONTACT_TABLE)

        val CREATE_CHAT_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_CHAT_SETTINGS(" +
                "$KEY_ID INTEGER PRIMARY KEY, " +
                "$KEY_CHAT_NOTIFICATIONS_ENABLED BOOLEAN, " +
                "$KEY_CHAT_SOUND_NOTIFICATIONS TEXT, " +
                "$KEY_CHAT_VIBRATION_ENABLED BOOLEAN, " +
                "$KEY_CHAT_VIDEO_QUALITY TEXT DEFAULT '${encrypt(VideoQuality.MEDIUM.value.toString())}')"
        db.execSQL(CREATE_CHAT_TABLE)

        val CREATE_COMPLETED_TRANSFER_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_COMPLETED_TRANSFERS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +                      //0
                    "$KEY_TRANSFER_FILENAME TEXT, " +                      //1
                    "$KEY_TRANSFER_TYPE TEXT, " +                          //2
                    "$KEY_TRANSFER_STATE TEXT, " +                         //3
                    "$KEY_TRANSFER_SIZE TEXT, " +                          //4
                    "$KEY_TRANSFER_HANDLE TEXT, " +                        //5
                    "$KEY_TRANSFER_PATH TEXT, " +                          //6
                    "$KEY_TRANSFER_OFFLINE BOOLEAN, " +                    //7
                    "$KEY_TRANSFER_TIMESTAMP TEXT, " +                     //8
                    "$KEY_TRANSFER_ERROR TEXT, " +                         //9
                    "$KEY_TRANSFER_ORIGINAL_PATH TEXT, " +                 //10
                    "$KEY_TRANSFER_PARENT_HANDLE TEXT)"                    //11
        db.execSQL(CREATE_COMPLETED_TRANSFER_TABLE)

        val CREATE_EPHEMERAL = "CREATE TABLE IF NOT EXISTS $TABLE_EPHEMERAL(" +
                "$KEY_ID INTEGER PRIMARY KEY, " +
                "$KEY_EMAIL TEXT, " +
                "$KEY_PASSWORD TEXT, " +
                "$KEY_SESSION TEXT, " +
                "$KEY_FIRST_NAME TEXT, " +
                "$KEY_LAST_NAME TEXT)"
        db.execSQL(CREATE_EPHEMERAL)

        val CREATE_PENDING_MSG_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_PENDING_MSG(" +
                "$KEY_ID INTEGER PRIMARY KEY," +
                "$KEY_ID_CHAT TEXT, " +
                "$KEY_MSG_TIMESTAMP TEXT, " +
                "$KEY_ID_TEMP_KARERE TEXT, " +
                "$KEY_STATE INTEGER)"
        db.execSQL(CREATE_PENDING_MSG_TABLE)

        val CREATE_MSG_NODE_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_MSG_NODES(" +
                "$KEY_ID INTEGER PRIMARY KEY," +
                "$KEY_ID_PENDING_MSG INTEGER, " +
                "$KEY_ID_NODE INTEGER)"
        db.execSQL(CREATE_MSG_NODE_TABLE)

        val CREATE_NODE_ATTACHMENTS_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NODE_ATTACHMENTS(" +
                    "$KEY_ID INTEGER PRIMARY KEY," +
                    "$KEY_FILE_PATH TEXT, " +
                    "$KEY_FILE_NAME TEXT, " +
                    "$KEY_FILE_FINGERPRINT TEXT, " +
                    "$KEY_NODE_HANDLE TEXT)"
        db.execSQL(CREATE_NODE_ATTACHMENTS_TABLE)

        val CREATE_NEW_PENDING_MSG_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_PENDING_MSG_SINGLE(" +
                    "$KEY_ID INTEGER PRIMARY KEY," +
                    "$KEY_PENDING_MSG_ID_CHAT TEXT, " +
                    "$KEY_PENDING_MSG_TIMESTAMP TEXT, " +
                    "$KEY_PENDING_MSG_TEMP_KARERE TEXT, " +
                    "$KEY_PENDING_MSG_FILE_PATH TEXT, " +
                    "$KEY_PENDING_MSG_NAME TEXT, " +
                    "$KEY_PENDING_MSG_NODE_HANDLE TEXT, " +
                    "$KEY_PENDING_MSG_FINGERPRINT TEXT, " +
                    "$KEY_PENDING_MSG_TRANSFER_TAG INTEGER, " +
                    "$KEY_PENDING_MSG_STATE INTEGER)"
        db.execSQL(CREATE_NEW_PENDING_MSG_TABLE)

        db.execSQL(CREATE_SYNC_RECORDS_TABLE)
        db.execSQL(CREATE_MEGA_CONTACTS_TABLE)
        db.execSQL(CREATE_SD_TRANSFERS_TABLE)
        db.execSQL(CREATE_BACKUP_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Timber.i("Database upgraded from %d to %d", oldVersion, newVersion)

        //Used to identify when the Chat Settings table has been already recreated
        var chatSettingsAlreadyUpdated = false
        //Used to identify when the Attributes table has been already recreated
        var attributesAlreadyUpdated = false
        //Used to identify when the Preferences table has been already recreated
        var preferencesAlreadyUpdated = false
        if (oldVersion <= 7) {
            db.execSQL("ALTER TABLE $TABLE_OFFLINE ADD COLUMN $KEY_OFF_INCOMING INTEGER;")
            db.execSQL("ALTER TABLE $TABLE_OFFLINE ADD COLUMN $KEY_OFF_HANDLE_INCOMING INTEGER;")
            db.execSQL("UPDATE $TABLE_OFFLINE SET $KEY_OFF_INCOMING = '0';")
        }
        if (oldVersion <= 8) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_LAST_UPLOAD_FOLDER TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_LAST_UPLOAD_FOLDER = '" + encrypt(
                "") + "';")
        }
        if (oldVersion <= 9) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_LAST_CLOUD_FOLDER_HANDLE TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_LAST_CLOUD_FOLDER_HANDLE = '" + encrypt(
                "") + "';")
        }
        if (oldVersion <= 12) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_SEC_FOLDER_ENABLED TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_SEC_FOLDER_LOCAL_PATH TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_SEC_FOLDER_HANDLE TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_SEC_SYNC_TIMESTAMP TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_KEEP_FILE_NAMES TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_ENABLED = '${encrypt("false")}';")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_LOCAL_PATH = '${encrypt("-1")}';")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_HANDLE = '${encrypt("-1")}';")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_SEC_SYNC_TIMESTAMP = '${encrypt("0")}';")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_KEEP_FILE_NAMES = '${encrypt("false")}';")
        }
        if (oldVersion <= 13) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_STORAGE_ADVANCED_DEVICES BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_STORAGE_ADVANCED_DEVICES = '${encrypt("false")}';")
        }
        if (oldVersion <= 14) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_ATTR_INTENTS TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_INTENTS = '${encrypt("0")}';")
        }
        if (oldVersion <= 15) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_PREFERRED_VIEW_LIST BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_VIEW_LIST = '${encrypt("true")}';")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_PREFERRED_VIEW_LIST_CAMERA BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_VIEW_LIST_CAMERA = '${encrypt("false")}';")
        }
        if (oldVersion <= 16) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_ATTR_ASK_SIZE_DOWNLOAD BOOLEAN;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_ASK_SIZE_DOWNLOAD = '${encrypt("true")}';")
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_ATTR_ASK_NOAPP_DOWNLOAD BOOLEAN;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_ASK_NOAPP_DOWNLOAD = '${encrypt("true")}';")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_URI_EXTERNAL_SD_CARD TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_URI_EXTERNAL_SD_CARD = '${encrypt("")}';")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD = '${
                encrypt("false")
            }';")
        }
        if (oldVersion <= 17) {
            val CREATE_CONTACTS_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_CONTACTS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +
                    "$KEY_CONTACT_HANDLE TEXT, " +
                    "$KEY_CONTACT_MAIL TEXT, " +
                    "$KEY_CONTACT_NAME TEXT, " +
                    "$KEY_CONTACT_LAST_NAME TEXT)"
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
        if (oldVersion <= 19) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_PASSCODE_LOCK_TYPE TEXT;")
            if (isPasscodeLockEnabled(db)) {
                Timber.d("PIN enabled!")
                db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_PASSCODE_LOCK_TYPE = '${
                    encrypt(Constants.PIN_4)
                }';")
            } else {
                Timber.d("PIN NOT enabled!")
                db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_PASSCODE_LOCK_TYPE = '${encrypt("")}';")
            }
        }
        if (oldVersion <= 20) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_PREFERRED_SORT_CLOUD TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_PREFERRED_SORT_OTHERS TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_SORT_CLOUD = '${
                encrypt(MegaApiJava.ORDER_DEFAULT_ASC.toString())
            }';")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_SORT_OTHERS = '${
                encrypt(MegaApiJava.ORDER_DEFAULT_ASC.toString())
            }';")
        }
        if (oldVersion <= 21) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_ACCOUNT_DETAILS_TIMESTAMP TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_ACCOUNT_DETAILS_TIMESTAMP = '${encrypt("")}';")
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_PAYMENT_METHODS_TIMESTAMP TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_PAYMENT_METHODS_TIMESTAMP = '${encrypt("")}';")
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_PRICING_TIMESTAMP TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_PRICING_TIMESTAMP = '${encrypt("")}';")
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP = '${
                encrypt("")
            }';")
        }
        if (oldVersion <= 22) {
            val CREATE_CHAT_ITEM_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_CHAT_ITEMS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +
                    "$KEY_CHAT_HANDLE TEXT, " +
                    "$KEY_CHAT_ITEM_NOTIFICATIONS BOOLEAN, " +
                    "$KEY_CHAT_ITEM_RINGTONE TEXT, " +
                    "$KEY_CHAT_ITEM_SOUND_NOTIFICATIONS TEXT)"
            db.execSQL(CREATE_CHAT_ITEM_TABLE)
            val CREATE_NONCONTACT_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_NON_CONTACTS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +
                    "$KEY_NONCONTACT_HANDLE TEXT, " +
                    "$KEY_NONCONTACT_FULLNAME TEXT)"
            db.execSQL(CREATE_NONCONTACT_TABLE)
            val CREATE_CHAT_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_CHAT_SETTINGS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +
                    "$KEY_CHAT_NOTIFICATIONS_ENABLED BOOLEAN, " +
                    "$KEY_CHAT_SOUND_NOTIFICATIONS TEXT, " +
                    "$KEY_CHAT_VIBRATION_ENABLED BOOLEAN)"
            db.execSQL(CREATE_CHAT_TABLE)
        }
        if (oldVersion <= 23) {
            db.execSQL("ALTER TABLE $TABLE_CREDENTIALS ADD COLUMN $KEY_FIRST_NAME TEXT;")
            db.execSQL("UPDATE $TABLE_CREDENTIALS SET $KEY_FIRST_NAME = '${encrypt("")}';")
            db.execSQL("ALTER TABLE $TABLE_CREDENTIALS ADD COLUMN $KEY_LAST_NAME TEXT;")
            db.execSQL("UPDATE $TABLE_CREDENTIALS SET $KEY_LAST_NAME = '${encrypt("")}';")
        }
        if (oldVersion <= 25) {
            db.execSQL("ALTER TABLE $TABLE_NON_CONTACTS ADD COLUMN $KEY_NONCONTACT_FIRSTNAME TEXT;")
            db.execSQL("UPDATE $TABLE_NON_CONTACTS SET $KEY_NONCONTACT_FIRSTNAME = '${encrypt("")}';")
            db.execSQL("ALTER TABLE $TABLE_NON_CONTACTS ADD COLUMN $KEY_NONCONTACT_LASTNAME TEXT;")
            db.execSQL("UPDATE $TABLE_NON_CONTACTS SET $KEY_NONCONTACT_LASTNAME = '${encrypt("")}';")
        }
        if (oldVersion <= 26) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_INVALIDATE_SDK_CACHE TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_INVALIDATE_SDK_CACHE = '${encrypt("true")}';")
        }
        if (oldVersion <= 27) {
            db.execSQL("ALTER TABLE $TABLE_NON_CONTACTS ADD COLUMN $KEY_NONCONTACT_EMAIL TEXT;")
            db.execSQL("UPDATE $TABLE_NON_CONTACTS SET $KEY_NONCONTACT_EMAIL = '${encrypt("")}';")
        }
        if (oldVersion <= 28) {
            db.execSQL("ALTER TABLE $TABLE_CREDENTIALS ADD COLUMN $KEY_MY_HANDLE TEXT;")
            db.execSQL("UPDATE $TABLE_CREDENTIALS SET $KEY_MY_HANDLE = '${encrypt("")}';")
        }
        if (oldVersion <= 29) {
            val CREATE_COMPLETED_TRANSFER_TABLE =
                "CREATE TABLE IF NOT EXISTS $TABLE_COMPLETED_TRANSFERS(" +
                        "$KEY_ID INTEGER PRIMARY KEY, " +
                        "$KEY_TRANSFER_FILENAME TEXT, " +
                        "$KEY_TRANSFER_TYPE TEXT, " +
                        "$KEY_TRANSFER_STATE TEXT, " +
                        "$KEY_TRANSFER_SIZE TEXT, " +
                        "$KEY_TRANSFER_HANDLE TEXT)"
            db.execSQL(CREATE_COMPLETED_TRANSFER_TABLE)
        }
        if (oldVersion <= 30) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_FIRST_LOGIN_CHAT BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_FIRST_LOGIN_CHAT = '${encrypt("true")}';")
        }
        if (oldVersion <= 31) {
            val CREATE_EPHEMERAL = "CREATE TABLE IF NOT EXISTS $TABLE_EPHEMERAL(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +
                    "$KEY_EMAIL TEXT, " +
                    "$KEY_PASSWORD TEXT, " +
                    "$KEY_SESSION TEXT, " +
                    "$KEY_FIRST_NAME TEXT, " +
                    "$KEY_LAST_NAME TEXT)"
            db.execSQL(CREATE_EPHEMERAL)
        }
        if (oldVersion <= 32) {
            val CREATE_PENDING_MSG_TABLE = ("CREATE TABLE IF NOT EXISTS $TABLE_PENDING_MSG(" +
                    "$KEY_ID INTEGER PRIMARY KEY," +
                    "$KEY_ID_CHAT TEXT, " +
                    "$KEY_MSG_TIMESTAMP TEXT, " +
                    "$KEY_ID_TEMP_KARERE TEXT, " +
                    "$KEY_STATE INTEGER)")
            db.execSQL(CREATE_PENDING_MSG_TABLE)

            val CREATE_MSG_NODE_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_MSG_NODES(" +
                    "$KEY_ID INTEGER PRIMARY KEY," +
                    "$KEY_ID_PENDING_MSG INTEGER, " +
                    "$KEY_ID_NODE INTEGER)"
            db.execSQL(CREATE_MSG_NODE_TABLE)

            val CREATE_NODE_ATTACHMENTS_TABLE =
                "CREATE TABLE IF NOT EXISTS $TABLE_NODE_ATTACHMENTS(" +
                        "$KEY_ID INTEGER PRIMARY KEY," +
                        "$KEY_FILE_PATH TEXT, " +
                        "$KEY_FILE_NAME TEXT, " +
                        "$KEY_FILE_FINGERPRINT TEXT, " +
                        "$KEY_NODE_HANDLE TEXT)"
            db.execSQL(CREATE_NODE_ATTACHMENTS_TABLE)
        }
        if (oldVersion <= 34) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_USE_HTTPS_ONLY TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_USE_HTTPS_ONLY = '${encrypt("false")}';")
        }
        if (oldVersion <= 35) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_SHOW_COPYRIGHT TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_SHOW_COPYRIGHT = '${encrypt("true")}';")
        }
        if (oldVersion <= 37) {
            db.execSQL("ALTER TABLE $TABLE_CHAT_ITEMS ADD COLUMN $KEY_CHAT_ITEM_WRITTEN_TEXT TEXT;")
            db.execSQL("UPDATE $TABLE_CHAT_ITEMS SET $KEY_CHAT_ITEM_WRITTEN_TEXT = '';")
        }
        if (oldVersion <= 38) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_SHOW_NOTIF_OFF TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_SHOW_NOTIF_OFF = '${encrypt("true")}';")
        }
        if (oldVersion <= 41) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_LAST_PUBLIC_HANDLE TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_LAST_PUBLIC_HANDLE = '${encrypt("-1")}';")
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_LAST_PUBLIC_HANDLE_TIMESTAMP TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_LAST_PUBLIC_HANDLE_TIMESTAMP = '${
                encrypt("-1")
            }';")
        }
        if (oldVersion <= 42) {
            val CREATE_NEW_PENDING_MSG_TABLE =
                "CREATE TABLE IF NOT EXISTS $TABLE_PENDING_MSG_SINGLE(" +
                        "$KEY_ID INTEGER PRIMARY KEY," +
                        "$KEY_PENDING_MSG_ID_CHAT TEXT, " +
                        "$KEY_PENDING_MSG_TIMESTAMP TEXT, " +
                        "$KEY_PENDING_MSG_TEMP_KARERE TEXT, " +
                        "$KEY_PENDING_MSG_FILE_PATH TEXT, " +
                        "$KEY_PENDING_MSG_NAME TEXT, " +
                        "$KEY_PENDING_MSG_NODE_HANDLE TEXT, " +
                        "$KEY_PENDING_MSG_FINGERPRINT TEXT, " +
                        "$KEY_PENDING_MSG_TRANSFER_TAG INTEGER, " +
                        "$KEY_PENDING_MSG_STATE INTEGER)"
            db.execSQL(CREATE_NEW_PENDING_MSG_TABLE)
        }
        if (oldVersion <= 43) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_AUTO_PLAY BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_AUTO_PLAY = '${encrypt("false")}';")
        }
        if (oldVersion <= 44) {
            db.execSQL(CREATE_SYNC_RECORDS_TABLE)
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_UPLOAD_VIDEO_QUALITY TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_CONVERSION_ON_CHARGING BOOLEAN;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_CHARGING_ON_SIZE TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_SHOULD_CLEAR_CAMSYNC_RECORDS TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_CAM_VIDEO_SYNC_TIMESTAMP TEXT;")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_SEC_VIDEO_SYNC_TIMESTAMP TEXT;")
        }
        if (oldVersion <= 45) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_REMOVE_GPS TEXT;")
            db.execSQL("UPDATE " + TABLE_PREFERENCES + " SET " + KEY_REMOVE_GPS + " = '" + encrypt("true") + "';")
        }
        if (oldVersion <= 46) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_STORAGE_STATE INTEGER;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_STORAGE_STATE = '${
                encrypt(storageStateIntMapper(StorageState.Unknown).toString())
            }';")
        }
        if (oldVersion <= 47) {
            db.execSQL(CREATE_MEGA_CONTACTS_TABLE)
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_SHOW_INVITE_BANNER TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_SHOW_INVITE_BANNER = '${encrypt("true")}';")
        }
        if (oldVersion <= 48) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_PREFERRED_SORT_CAMERA_UPLOAD TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES " +
                    "SET $KEY_PREFERRED_SORT_CAMERA_UPLOAD = " +
                    "'${encrypt(MegaApiJava.ORDER_MODIFICATION_DESC.toString())}';")
        }
        if (oldVersion <= 49) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_SD_CARD_URI TEXT;")
        }
        if (oldVersion <= 50) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_ASK_FOR_DISPLAY_OVER TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_ASK_FOR_DISPLAY_OVER = '${encrypt("true")}';")
        }
        if (oldVersion <= 51) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_LAST_PUBLIC_HANDLE_TYPE INTEGER;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES " +
                    "SET $KEY_LAST_PUBLIC_HANDLE_TYPE = " +
                    "'${encrypt(MegaApiJava.AFFILIATE_TYPE_INVALID.toString())}';")
        }
        if (oldVersion <= 52) {
            recreateChatSettings(db, getChatSettingsFromDBv52(db))
            chatSettingsAlreadyUpdated = true
        }
        if (oldVersion <= 53) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_ASK_SET_DOWNLOAD_LOCATION BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_ASK_SET_DOWNLOAD_LOCATION = '${encrypt("true")}';")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_STORAGE_ASK_ALWAYS = '${encrypt("true")}';")
            db.execSQL("ALTER TABLE $TABLE_COMPLETED_TRANSFERS ADD COLUMN $KEY_TRANSFER_PATH TEXT;")
        }
        if (oldVersion <= 54) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_MY_CHAT_FILES_FOLDER_HANDLE TEXT;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES " +
                    "SET $KEY_MY_CHAT_FILES_FOLDER_HANDLE = '${encrypt(MegaApiJava.INVALID_HANDLE.toString())}';")
        }
        if (oldVersion <= 55) {
            db.execSQL("ALTER TABLE $TABLE_CONTACTS ADD COLUMN $KEY_CONTACT_NICKNAME TEXT;")
        }
        if (oldVersion <= 56) {
            db.execSQL("ALTER TABLE $TABLE_CHAT_ITEMS ADD COLUMN $KEY_CHAT_ITEM_EDITED_MSG_ID TEXT;")
            db.execSQL("UPDATE $TABLE_CHAT_ITEMS SET $KEY_CHAT_ITEM_EDITED_MSG_ID = '';")
        }
        if (oldVersion <= 57) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_URI_MEDIA_EXTERNAL_SD_CARD TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_URI_MEDIA_EXTERNAL_SD_CARD = '${encrypt("")}';")
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES " +
                    "SET $KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD = '${encrypt("false")}';")
            db.execSQL("ALTER TABLE $TABLE_COMPLETED_TRANSFERS ADD COLUMN $KEY_TRANSFER_OFFLINE BOOLEAN;")
            db.execSQL("UPDATE $TABLE_COMPLETED_TRANSFERS SET $KEY_TRANSFER_OFFLINE = '${encrypt("false")}';")
            db.execSQL("ALTER TABLE $TABLE_COMPLETED_TRANSFERS ADD COLUMN $KEY_TRANSFER_TIMESTAMP TEXT;")
            db.execSQL("UPDATE $TABLE_COMPLETED_TRANSFERS " +
                    "SET $KEY_TRANSFER_TIMESTAMP = '${
                        encrypt(System.currentTimeMillis().toString())
                    }';")
            db.execSQL("ALTER TABLE $TABLE_COMPLETED_TRANSFERS ADD COLUMN $KEY_TRANSFER_ERROR TEXT;")
            db.execSQL("UPDATE $TABLE_COMPLETED_TRANSFERS SET $KEY_TRANSFER_ERROR = '${encrypt("")}';")
            db.execSQL("ALTER TABLE $TABLE_COMPLETED_TRANSFERS ADD COLUMN $KEY_TRANSFER_ORIGINAL_PATH TEXT;")
            db.execSQL("UPDATE $TABLE_COMPLETED_TRANSFERS SET $KEY_TRANSFER_ORIGINAL_PATH = '${
                encrypt("")
            }';")
            db.execSQL("ALTER TABLE $TABLE_COMPLETED_TRANSFERS ADD COLUMN $KEY_TRANSFER_PARENT_HANDLE TEXT;")
            db.execSQL("UPDATE $TABLE_COMPLETED_TRANSFERS SET $KEY_TRANSFER_PARENT_HANDLE = '${
                encrypt(MegaApiJava.INVALID_HANDLE.toString())
            }';")
        }
        if (oldVersion <= 58) {
            db.execSQL("ALTER TABLE $TABLE_ATTRIBUTES ADD COLUMN $KEY_TRANSFER_QUEUE_STATUS BOOLEAN;")
            db.execSQL("UPDATE $TABLE_ATTRIBUTES SET $KEY_TRANSFER_QUEUE_STATUS = '${encrypt("false")}';")
            db.execSQL(CREATE_SD_TRANSFERS_TABLE)
        }
        if (oldVersion <= 59) {
            db.execSQL(CREATE_BACKUP_TABLE)
        }
        if (oldVersion <= 60) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_PASSCODE_LOCK_REQUIRE_TIME TEXT;")
            db.execSQL("UPDATE $TABLE_PREFERENCES " +
                    "SET $KEY_PASSCODE_LOCK_REQUIRE_TIME = '${
                        encrypt("" + if (isPasscodeLockEnabled(db)) PasscodeUtil.REQUIRE_PASSCODE_IMMEDIATE else Constants.REQUIRE_PASSCODE_INVALID)
                    }';")
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
        if (oldVersion <= 63 && !preferencesAlreadyUpdated) {
            db.execSQL("ALTER TABLE $TABLE_PREFERENCES ADD COLUMN $KEY_FINGERPRINT_LOCK BOOLEAN;")
            db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_FINGERPRINT_LOCK = '${encrypt("false")}';")
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
        this.db = db
    }

    /**
     * Drops the chat settings table if exists, creates the new one,
     * and then sets the updated chat settings.
     *
     * @param db           Current DB.
     * @param chatSettings Chat Settings.
     */
    private fun recreateChatSettings(db: SQLiteDatabase, chatSettings: ChatSettings?) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_SETTINGS")
        onCreate(db)
        chatSettings?.let { setChatSettings(db, it) }

        // Temporary fix to avoid wrong values in chat settings after upgrade.
        getChatSettings(db)
    }

    /**
     * Drops the attributes table if exists, creates the new one,
     * and then sets the updated attributes.
     *
     * @param db   Current DB.
     * @param attr Attributes.
     */
    private fun recreateAttributes(db: SQLiteDatabase, attr: MegaAttributes?) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTRIBUTES")
        onCreate(db)
        attr?.let { setAttributes(db, it) }

        // Temporary fix to avoid wrong values in attributes after upgrade.
        getAttributes(db)
    }

    /**
     * Drops the preferences table if exists, creates the new one,
     * and then sets the updated preferences.
     *
     * @param db          Current DB.
     * @param preferences Preferences.
     */
    private fun recreatePreferences(db: SQLiteDatabase, preferences: MegaPreferences?) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PREFERENCES")
        onCreate(db)
        preferences?.let { setPreferences(db, it) }

        // Temporary fix to avoid wrong values in preferences after upgrade.
        getPreferences(db)
    }

    override fun saveCredentials(userCredentials: UserCredentials) {
        val values = ContentValues().apply {
            with(userCredentials) {
                put(KEY_ID, 1)
                email?.let { put(KEY_EMAIL, encrypt(it)) }
                session?.let { put(KEY_SESSION, encrypt(it)) }
                myHandle?.let { put(KEY_MY_HANDLE, encrypt(it)) }
            }
        }
        db.insertWithOnConflict(TABLE_CREDENTIALS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun saveSyncRecord(record: SyncRecord) {
        val values = ContentValues().apply {
            with(record) {
                localPath?.let { put(KEY_SYNC_FILEPATH_ORI, encrypt(it)) }
                newPath?.let { put(KEY_SYNC_FILEPATH_NEW, encrypt(it)) }
                originFingerprint?.let { put(KEY_SYNC_FP_ORI, encrypt(it)) }
                newFingerprint?.let { put(KEY_SYNC_FP_NEW, encrypt(it)) }
                fileName?.let { put(KEY_SYNC_FILENAME, encrypt(it)) }
                nodeHandle?.let { put(KEY_SYNC_HANDLE, encrypt(it.toString())) }
                timestamp?.let { put(KEY_SYNC_TIMESTAMP, encrypt(it.toString())) }
                put(KEY_SYNC_COPYONLY, encrypt(record.isCopyOnly.toString()))
                put(KEY_SYNC_SECONDARY, encrypt(record.isSecondary.toString()))
                longitude?.let { put(KEY_SYNC_LONGITUDE, encrypt(it.toString())) }
                latitude?.let { put(KEY_SYNC_LATITUDE, encrypt(it.toString())) }
                put(KEY_SYNC_STATE, record.status)
                put(KEY_SYNC_TYPE, record.type)
            }
        }
        db.insert(TABLE_SYNC_RECORDS, null, values)
    }

    override fun updateVideoState(state: Int) {
        val sql =
            "UPDATE $TABLE_SYNC_RECORDS SET $KEY_SYNC_STATE = $state  " +
                    "WHERE $KEY_SYNC_TYPE = $SYNC_RECORD_TYPE_VIDEO"
        db.execSQL(sql)
    }

    override fun fileNameExists(name: String?, isSecondary: Boolean, fileType: Int): Boolean {
        var selectQuery = "SELECT * FROM $TABLE_SYNC_RECORDS " +
                "WHERE $KEY_SYNC_FILENAME ='${encrypt(name)}' " +
                "AND $KEY_SYNC_SECONDARY = '${encrypt(isSecondary.toString())}'"
        if (fileType != SYNC_RECORD_TYPE_ANY) {
            selectQuery += " AND $KEY_SYNC_TYPE = $fileType"
        }
        db.rawQuery(selectQuery, null).use { cursor -> return cursor != null && cursor.count == 1 }
    }

    override fun localPathExists(localPath: String?, isSecondary: Boolean, fileType: Int): Boolean {
        var selectQuery = ("SELECT * FROM $TABLE_SYNC_RECORDS " +
                "WHERE $KEY_SYNC_FILEPATH_ORI ='${encrypt(localPath)}' " +
                "AND $KEY_SYNC_SECONDARY = '${encrypt(isSecondary.toString())}'")
        if (fileType != SYNC_RECORD_TYPE_ANY) {
            selectQuery += " AND $KEY_SYNC_TYPE = $fileType"
        }
        db.rawQuery(selectQuery, null).use { cursor ->
            return cursor != null && cursor.count == 1
        }
    }

    override fun recordExists(
        originalFingerprint: String?,
        isSecondary: Boolean,
        isCopyOnly: Boolean,
    ): SyncRecord? {
        val selectQuery = ("SELECT * FROM $TABLE_SYNC_RECORDS " +
                "WHERE $KEY_SYNC_FP_ORI ='${encrypt(originalFingerprint)}' " +
                "AND $KEY_SYNC_SECONDARY = '${encrypt(isSecondary.toString())}' " +
                "AND $KEY_SYNC_COPYONLY = '${encrypt(isCopyOnly.toString())}'")
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return extractSyncRecord(cursor)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun findAllPendingSyncRecords(): List<SyncRecord> {
        val selectQuery = "SELECT * FROM $TABLE_SYNC_RECORDS " +
                "WHERE $KEY_SYNC_STATE = ${SyncStatus.STATUS_PENDING.value}"
        val records: MutableList<SyncRecord> = ArrayList()
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val record = extractSyncRecord(cursor)
                        records.add(record)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return records
    }

    override fun findVideoSyncRecordsByState(state: Int): List<SyncRecord> {
        val selectQuery = "SELECT * FROM $TABLE_SYNC_RECORDS " +
                "WHERE $KEY_SYNC_STATE = $state " +
                "AND $KEY_SYNC_TYPE = $SYNC_RECORD_TYPE_VIDEO"
        val records: MutableList<SyncRecord> = ArrayList()
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val record = extractSyncRecord(cursor)
                        records.add(record)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return records
    }

    /**
     * Delete all sync records by a given sync record type
     */
    override fun deleteAllSyncRecords(fileType: Int) {
        var sql = "DELETE FROM $TABLE_SYNC_RECORDS"
        if (fileType != SYNC_RECORD_TYPE_ANY) {
            sql += " WHERE $KEY_SYNC_TYPE = $fileType"
        }
        db.execSQL(sql)
    }

    /**
     * Delete all sync records with any type
     */
    override fun deleteAllSyncRecordsTypeAny() {
        val sql = "DELETE FROM $TABLE_SYNC_RECORDS"
        db.execSQL(sql)
    }

    /**
     * Delete all secondary sync records with any type
     */
    override fun deleteAllSecondarySyncRecords() {
        val sql = "DELETE FROM $TABLE_SYNC_RECORDS WHERE $KEY_SYNC_SECONDARY ='${encrypt("true")}'"
        db.execSQL(sql)
    }

    /**
     * Delete all primary sync records with any type
     */
    override fun deleteAllPrimarySyncRecords() {
        val sql = "DELETE FROM $TABLE_SYNC_RECORDS WHERE $KEY_SYNC_SECONDARY ='${encrypt("false")}'"
        db.execSQL(sql)
    }

    override fun deleteVideoRecordsByState(state: Int) {
        val sql = "DELETE FROM $TABLE_SYNC_RECORDS " +
                "WHERE $KEY_SYNC_STATE = $state " +
                "AND $KEY_SYNC_TYPE = $SYNC_RECORD_TYPE_VIDEO"
        db.execSQL(sql)
    }

    private fun extractSyncRecord(cursor: Cursor): SyncRecord {
        val timestamp = decrypt(cursor.getString(5))
        val longitude = decrypt(cursor.getString(7))
        val latitude = decrypt(cursor.getString(8))
        val nodeHandle = decrypt(cursor.getString(11))
        return SyncRecord(
            id = cursor.getInt(0),
            localPath = decrypt(cursor.getString(1)),
            newPath = decrypt(cursor.getString(2)),
            originFingerprint = decrypt(cursor.getString(3)),
            newFingerprint = decrypt(cursor.getString(4)),
            timestamp = if (!timestamp.isNullOrEmpty()) timestamp.toLong() else null,
            fileName = decrypt(cursor.getString(6)),
            longitude = if (!longitude.isNullOrEmpty()) longitude.toFloat() else null,
            latitude = if (!latitude.isNullOrEmpty()) latitude.toFloat() else null,
            status = cursor.getInt(9),
            type = cursor.getInt(10),
            nodeHandle = if (!nodeHandle.isNullOrEmpty()) nodeHandle.toLong() else null,
            isCopyOnly = java.lang.Boolean.parseBoolean(decrypt(cursor.getString(12))),
            isSecondary = java.lang.Boolean.parseBoolean(decrypt(cursor.getString(13))))
    }

    override fun findSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean): SyncRecord? {
        val selectQuery = "SELECT * FROM $TABLE_SYNC_RECORDS " +
                "WHERE $KEY_SYNC_FILEPATH_ORI ='${encrypt(localPath)}' " +
                "AND $KEY_SYNC_SECONDARY ='${encrypt(isSecondary.toString())}'"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return extractSyncRecord(cursor)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean) {
        val sql = "DELETE FROM $TABLE_SYNC_RECORDS  " +
                "WHERE ($KEY_SYNC_FILEPATH_ORI ='${encrypt(path)}' " +
                "OR $KEY_SYNC_FILEPATH_NEW ='${encrypt(path)}') " +
                "AND $KEY_SYNC_SECONDARY ='${encrypt(isSecondary.toString())}'"
        db.execSQL(sql)
    }

    override fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean) {
        val sql = "DELETE FROM $TABLE_SYNC_RECORDS  " +
                "WHERE $KEY_SYNC_FILEPATH_ORI ='${encrypt(localPath)}' " +
                "AND $KEY_SYNC_SECONDARY ='${encrypt(isSecondary.toString())}'"
        db.execSQL(sql)
    }

    override fun deleteSyncRecordByNewPath(newPath: String?) {
        val sql =
            "DELETE FROM $TABLE_SYNC_RECORDS WHERE $KEY_SYNC_FILEPATH_NEW ='${encrypt(newPath)}'"
        db.execSQL(sql)
    }

    override fun deleteSyncRecordByFileName(fileName: String?) {
        val sql = "DELETE FROM $TABLE_SYNC_RECORDS  " +
                "WHERE $KEY_SYNC_FILENAME = '${encrypt(fileName)}' " +
                "OR $KEY_SYNC_FILEPATH_ORI LIKE '%${encrypt(fileName)}'"
        db.execSQL(sql)
    }

    override fun deleteSyncRecordByFingerprint(
        oriFingerprint: String?,
        newFingerprint: String?,
        isSecondary: Boolean,
    ) {
        val sql = ("DELETE FROM $TABLE_SYNC_RECORDS  " +
                "WHERE $KEY_SYNC_FP_ORI = '${encrypt(oriFingerprint)}' " +
                "OR $KEY_SYNC_FP_NEW = '${encrypt(newFingerprint)}' " +
                "AND $KEY_SYNC_SECONDARY ='${encrypt(isSecondary.toString())}'")
        db.execSQL(sql)
    }

    override fun updateSyncRecordStatusByLocalPath(
        status: Int,
        localPath: String?,
        isSecondary: Boolean,
    ) {
        val sql = "UPDATE $TABLE_SYNC_RECORDS SET $KEY_SYNC_STATE = $status  " +
                "WHERE $KEY_SYNC_FILEPATH_ORI = '${encrypt(localPath)}' " +
                "AND $KEY_SYNC_SECONDARY ='${encrypt(isSecondary.toString())}'"
        db.execSQL(sql)
    }

    override fun findSyncRecordByNewPath(newPath: String?): SyncRecord? {
        val selectQuery = ("SELECT * FROM $TABLE_SYNC_RECORDS " +
                "WHERE $KEY_SYNC_FILEPATH_NEW ='${encrypt(newPath)}'")
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return extractSyncRecord(cursor)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun shouldClearCamsyncRecords(): Boolean {
        val selectQuery =
            "SELECT $KEY_SHOULD_CLEAR_CAMSYNC_RECORDS FROM $TABLE_PREFERENCES"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    var should = cursor.getString(cursor.getColumnIndexOrThrow(
                        KEY_SHOULD_CLEAR_CAMSYNC_RECORDS))
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
        db.execSQL(sql)
    }

    override fun findMaxTimestamp(isSecondary: Boolean, fileType: Int): Long? {
        val selectQuery = "SELECT $KEY_SYNC_TIMESTAMP FROM $TABLE_SYNC_RECORDS  " +
                "WHERE $KEY_SYNC_SECONDARY = '${encrypt(isSecondary.toString())}' " +
                "AND $KEY_SYNC_TYPE = $fileType"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_UPLOAD_VIDEO_QUALITY= '${encrypt(quality.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_UPLOAD_VIDEO_QUALITY, encrypt(quality.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CONVERSION_ON_CHARGING= '${
                            encrypt(onCharging.toString())
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CONVERSION_ON_CHARGING, encrypt(onCharging.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CHARGING_ON_SIZE= '${encrypt(size.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CHARGING_ON_SIZE, encrypt(size.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_REMOVE_GPS= '${encrypt(removeGPS.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_REMOVE_GPS, encrypt(removeGPS.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun saveEphemeral(ephemeralCredentials: EphemeralCredentials) {
        val values = ContentValues().apply {
            with(ephemeralCredentials) {
                email?.let { put(KEY_EMAIL, encrypt(it)) }
                password?.let { put(KEY_PASSWORD, encrypt(it)) }
                session?.let { put(KEY_SESSION, encrypt(it)) }
                firstName?.let { put(KEY_FIRST_NAME, encrypt(it)) }
                lastName?.let { put(KEY_LAST_NAME, encrypt(it)) }
            }
        }
        db.insert(TABLE_EPHEMERAL, null, values)
    }

    override fun saveMyEmail(email: String?) {
        Timber.d("saveEmail: %s", email)
        val selectQuery = "SELECT * FROM $TABLE_CREDENTIALS"
        val values = ContentValues()
        try {
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_CREDENTIALS_TABLE =
                        "UPDATE $TABLE_CREDENTIALS SET $KEY_EMAIL= '${encrypt(email)}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_CREDENTIALS_TABLE)
                } else {
                    values.put(KEY_EMAIL, encrypt(email))
                    db.insert(TABLE_CREDENTIALS, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_CREDENTIALS_TABLE =
                        "UPDATE $TABLE_CREDENTIALS SET $KEY_FIRST_NAME= '${encrypt(firstName)}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_CREDENTIALS_TABLE)
                } else {
                    values.put(KEY_FIRST_NAME, encrypt(firstName))
                    db.insert(TABLE_CREDENTIALS, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_CREDENTIALS_TABLE =
                        "UPDATE $TABLE_CREDENTIALS SET $KEY_LAST_NAME= '${encrypt(lastName)}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_CREDENTIALS_TABLE)
                } else {
                    values.put(KEY_LAST_NAME, encrypt(lastName))
                    db.insert(TABLE_CREDENTIALS, null, values)
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
                db.rawQuery(selectQuery, null)?.use { cursor ->
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
                db.rawQuery(selectQuery, null)?.use { cursor ->
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
                onCreate(db)
            } catch (e: Exception) {
                Timber.e(e, "Error decrypting DB field")
            }
            return userCredentials
        }

    override fun batchInsertMegaContacts(contacts: List<MegaContact>?) {
        if (contacts == null || contacts.isEmpty()) {
            Timber.w("Empty MEGA contacts list.")
            return
        }
        Timber.d("Contacts size is: %s", contacts.size)
        db.beginTransaction()
        try {
            var values: ContentValues
            for ((id, handle, localName, email, normalizedPhoneNumber) in contacts) {
                values = ContentValues().apply {
                    put(KEY_MEGA_CONTACTS_ID, encrypt(
                        id))
                    put(KEY_MEGA_CONTACTS_HANDLE, encrypt(
                        handle.toString()))
                    put(KEY_MEGA_CONTACTS_LOCAL_NAME, encrypt(
                        localName))
                    put(KEY_MEGA_CONTACTS_EMAIL, encrypt(
                        email))
                    put(KEY_MEGA_CONTACTS_PHONE_NUMBER, encrypt(
                        normalizedPhoneNumber))
                }
                db.insert(TABLE_MEGA_CONTACTS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override val megaContacts: ArrayList<MegaContact>
        get() {
            val sql = "SELECT * FROM $TABLE_MEGA_CONTACTS"
            val contacts = ArrayList<MegaContact>()
            try {
                db.rawQuery(sql, null)?.use { cursor ->
                    var contact: MegaContact
                    while (cursor.moveToNext()) {
                        contact = MegaContact()
                        try {
                            val id =
                                cursor.getString(cursor.getColumnIndexOrThrow(KEY_MEGA_CONTACTS_ID))
                            contact.id = decrypt(id)
                        } catch (exception: IllegalArgumentException) {
                            Timber.e(exception,
                                "Exception getting MEGA contact ID. Contact will not be included")
                            continue
                        }
                        try {
                            val handle =
                                cursor.getString(cursor.getColumnIndexOrThrow(
                                    KEY_MEGA_CONTACTS_HANDLE))
                            contact.handle = decrypt(handle)?.toLong() ?: 0
                        } catch (exception: IllegalArgumentException) {
                            Timber.e(exception,
                                "Exception getting MEGA contact handle. Contact will not be included")
                            continue
                        }
                        try {
                            val localName = cursor.getString(cursor.getColumnIndexOrThrow(
                                KEY_MEGA_CONTACTS_LOCAL_NAME))
                            contact.localName = decrypt(localName)
                        } catch (exception: IllegalArgumentException) {
                            Timber.w(exception, "Exception getting MEGA contact local name")
                        }
                        try {
                            val email =
                                cursor.getString(cursor.getColumnIndexOrThrow(
                                    KEY_MEGA_CONTACTS_EMAIL))
                            contact.email = decrypt(email)
                        } catch (exception: IllegalArgumentException) {
                            Timber.w(exception, "Exception getting MEGA contact email")
                        }
                        try {
                            val phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(
                                KEY_MEGA_CONTACTS_PHONE_NUMBER))
                            contact.normalizedPhoneNumber = decrypt(phoneNumber)
                        } catch (exception: IllegalArgumentException) {
                            Timber.w(exception, "Exception getting MEGA contact phone number")
                        }
                        contacts.add(contact)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return contacts
        }

    override fun clearMegaContacts() {
        Timber.d("delete table %s", TABLE_MEGA_CONTACTS)
        db.execSQL("DELETE FROM $TABLE_MEGA_CONTACTS")
    }

    override val ephemeral: EphemeralCredentials?
        get() {
            var ephemeralCredentials: EphemeralCredentials? = null
            val selectQuery = "SELECT * FROM $TABLE_EPHEMERAL"
            try {
                db.rawQuery(selectQuery, null)?.use { cursor ->
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
                onCreate(db)
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return ephemeralCredentials
        }

    /**
     * Sets preferences.
     *
     * @param db    Current DB.
     * @param prefs Preferences.
     */
    private fun setPreferences(db: SQLiteDatabase, prefs: MegaPreferences) {
        val values = ContentValues().apply {
            put(KEY_FIRST_LOGIN, encrypt(prefs.getFirstTime()))
            put(KEY_CAM_SYNC_WIFI, encrypt(prefs.getCamSyncWifi()))
            put(KEY_CAM_SYNC_ENABLED, encrypt(prefs.getCamSyncEnabled()))
            put(KEY_CAM_SYNC_HANDLE, encrypt(prefs.getCamSyncHandle()))
            put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(prefs.getCamSyncLocalPath()))
            put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(prefs.getCamSyncFileUpload()))
            put(KEY_PASSCODE_LOCK_ENABLED, encrypt(prefs.getPasscodeLockEnabled()))
            put(KEY_PASSCODE_LOCK_CODE, encrypt(prefs.getPasscodeLockCode()))
            put(KEY_STORAGE_ASK_ALWAYS, encrypt(prefs.getStorageAskAlways()))
            put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(prefs.getStorageDownloadLocation()))
            put(KEY_CAM_SYNC_TIMESTAMP, encrypt(prefs.getCamSyncTimeStamp()))
            put(KEY_CAM_VIDEO_SYNC_TIMESTAMP, encrypt(prefs.getCamVideoSyncTimeStamp()))
            put(KEY_LAST_UPLOAD_FOLDER, encrypt(prefs.getLastFolderUpload()))
            put(KEY_LAST_CLOUD_FOLDER_HANDLE, encrypt(prefs.getLastFolderCloud()))
            put(KEY_SEC_FOLDER_ENABLED, encrypt(prefs.getSecondaryMediaFolderEnabled()))
            put(KEY_SEC_FOLDER_LOCAL_PATH, encrypt(prefs.getLocalPathSecondaryFolder()))
            put(KEY_SEC_FOLDER_HANDLE, encrypt(prefs.getMegaHandleSecondaryFolder()))
            put(KEY_SEC_SYNC_TIMESTAMP, encrypt(prefs.getSecSyncTimeStamp()))
            put(KEY_SEC_VIDEO_SYNC_TIMESTAMP, encrypt(prefs.getSecVideoSyncTimeStamp()))
            put(KEY_STORAGE_ADVANCED_DEVICES, encrypt(prefs.getStorageAdvancedDevices()))
            put(KEY_PREFERRED_VIEW_LIST, encrypt(prefs.getPreferredViewList()))
            put(KEY_PREFERRED_VIEW_LIST_CAMERA,
                encrypt(prefs.getPreferredViewListCameraUploads()))
            put(KEY_URI_EXTERNAL_SD_CARD, encrypt(prefs.getUriExternalSDCard()))
            put(KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD,
                encrypt(prefs.getCameraFolderExternalSDCard()))
            put(KEY_PASSCODE_LOCK_TYPE, encrypt(prefs.getPasscodeLockType()))
            put(KEY_PREFERRED_SORT_CLOUD, encrypt(prefs.getPreferredSortCloud()))
            put(KEY_PREFERRED_SORT_CAMERA_UPLOAD, encrypt(prefs.preferredSortCameraUpload))
            put(KEY_PREFERRED_SORT_OTHERS, encrypt(prefs.getPreferredSortOthers()))
            put(KEY_FIRST_LOGIN_CHAT, encrypt(prefs.getFirstTimeChat()))
            put(KEY_REMOVE_GPS, encrypt(prefs.removeGPS))
            put(KEY_KEEP_FILE_NAMES, encrypt(prefs.getKeepFileNames()))
            put(KEY_AUTO_PLAY, encrypt(prefs.isAutoPlayEnabled().toString()))
            put(KEY_UPLOAD_VIDEO_QUALITY, encrypt(prefs.getUploadVideoQuality()))
            put(KEY_CONVERSION_ON_CHARGING, encrypt(prefs.getConversionOnCharging()))
            put(KEY_CHARGING_ON_SIZE, encrypt(prefs.getChargingOnSize()))
            put(KEY_SHOULD_CLEAR_CAMSYNC_RECORDS,
                encrypt(prefs.getShouldClearCameraSyncRecords()))
            put(KEY_SHOW_INVITE_BANNER, encrypt(prefs.showInviteBanner))
            put(KEY_SD_CARD_URI, encrypt(prefs.getSdCardUri()))
            put(KEY_ASK_FOR_DISPLAY_OVER, encrypt(prefs.askForDisplayOver))
            put(KEY_ASK_SET_DOWNLOAD_LOCATION, encrypt(prefs.askForSetDownloadLocation))
            put(KEY_URI_MEDIA_EXTERNAL_SD_CARD, encrypt(prefs.mediaSDCardUri))
            put(KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD, encrypt(prefs.isMediaOnSDCard))
            put(KEY_PASSCODE_LOCK_REQUIRE_TIME, encrypt(prefs.passcodeLockRequireTime))
            put(KEY_FINGERPRINT_LOCK, encrypt(prefs.fingerprintLock))
        }

        db.insert(TABLE_PREFERENCES, null, values)
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
        db.execSQL("UPDATE $TABLE_PREFERENCES SET $KEY_ASK_FOR_DISPLAY_OVER = '${encrypt("false")}';")
    }

    /**
     * Gets preferences from the DB v62 (previous to add four available video qualities).
     *
     * @param db Current DB.
     * @return Preferences.
     */
    private fun getPreferencesFromDBv62(db: SQLiteDatabase): MegaPreferences? {
        Timber.d("getPreferencesFromDBv62")

        return getPreferences(db)?.also { pref ->
            val uploadVideoQuality = pref.getUploadVideoQuality()
            if (!TextUtil.isTextEmpty(uploadVideoQuality)
                && uploadVideoQuality.toInt() == OLD_VIDEO_QUALITY_ORIGINAL
            ) {
                pref.setUploadVideoQuality(VideoQuality.ORIGINAL.value.toString())
            }
        }
    }

    /**
     * Gets preferences.
     *
     * @return Preferences.
     */
    override val preferences: MegaPreferences?
        get() = getPreferences(db)

    /**
     * Gets preferences.
     *
     * @param db Current DB.
     * @return Preferences.
     */
    private fun getPreferences(db: SQLiteDatabase): MegaPreferences? {
        var prefs: MegaPreferences? = null
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                    val downloadLocation = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_STORAGE_DOWNLOAD_LOCATION)))
                    val camSyncTimeStamp =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_CAM_SYNC_TIMESTAMP)))
                    val lastFolderUpload =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_UPLOAD_FOLDER)))
                    val lastFolderCloud = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_LAST_CLOUD_FOLDER_HANDLE)))
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
                    val storageAdvancedDevices = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_STORAGE_ADVANCED_DEVICES)))
                    val preferredViewList =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_PREFERRED_VIEW_LIST)))
                    val preferredViewListCamera = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_PREFERRED_VIEW_LIST_CAMERA)))
                    val uriExternalSDCard =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_URI_EXTERNAL_SD_CARD)))
                    val cameraFolderExternalSDCard = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD)))
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
                    val conversionOnCharging = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_CONVERSION_ON_CHARGING)))
                    val chargingOnSize =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_CHARGING_ON_SIZE)))
                    val shouldClearCameraSyncRecords = decrypt(cursor.getString(getColumnIndex(
                        cursor,
                        KEY_SHOULD_CLEAR_CAMSYNC_RECORDS)))
                    val camVideoSyncTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_CAM_VIDEO_SYNC_TIMESTAMP)))
                    val secVideoSyncTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_SEC_VIDEO_SYNC_TIMESTAMP)))
                    val removeGPS =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_REMOVE_GPS)))
                    val closeInviteBanner =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_INVITE_BANNER)))
                    val preferredSortCameraUpload = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_PREFERRED_SORT_CAMERA_UPLOAD)))
                    val sdCardUri =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SD_CARD_URI)))
                    val askForDisplayOver =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_ASK_FOR_DISPLAY_OVER)))
                    val askForSetDownloadLocation = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_ASK_SET_DOWNLOAD_LOCATION)))
                    val mediaSDCardUri = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_URI_MEDIA_EXTERNAL_SD_CARD)))
                    val isMediaOnSDCard = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD)))
                    val passcodeLockRequireTime = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_PASSCODE_LOCK_REQUIRE_TIME)))
                    val fingerprintLock =
                        if (cursor.getColumnIndex(KEY_FINGERPRINT_LOCK) != Constants.INVALID_VALUE) decrypt(
                            cursor.getString(getColumnIndex(cursor,
                                KEY_FINGERPRINT_LOCK))) else "false"
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
                        fingerprintLock)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return prefs
    }

    /**
     * Get chat settings from the DB v52 (previous to remove the setting to enable/disable the chat).
     * KEY_CHAT_ENABLED and KEY_CHAT_STATUS have been removed in DB v53.
     *
     * @return Chat settings.
     */
    private fun getChatSettingsFromDBv52(db: SQLiteDatabase): ChatSettings? {
        Timber.d("getChatSettings")
        var chatSettings: ChatSettings? = null
        val selectQuery = "SELECT * FROM $TABLE_CHAT_SETTINGS"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val notificationSound = decrypt(cursor.getString(3))
                    val vibrationEnabled = decrypt(cursor.getString(4))
                    val sendOriginalAttachments = decrypt(cursor.getString(6))
                    val videoQuality =
                        if (sendOriginalAttachments.toBoolean()) VideoQuality.ORIGINAL.value.toString() else VideoQuality.MEDIUM.value.toString()
                    chatSettings = ChatSettings(
                        notificationSound,
                        vibrationEnabled,
                        videoQuality)
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
    private fun getChatSettingsFromDBv62(db: SQLiteDatabase): ChatSettings? {
        Timber.d("getChatSettings")
        var chatSettings: ChatSettings? = null
        val selectQuery = "SELECT * FROM $TABLE_CHAT_SETTINGS"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val notificationSound = decrypt(cursor.getString(2))
                    val vibrationEnabled = decrypt(cursor.getString(3))
                    val sendOriginalAttachments = decrypt(cursor.getString(4))
                    val videoQuality =
                        if (sendOriginalAttachments.toBoolean()) VideoQuality.ORIGINAL.value.toString() else VideoQuality.MEDIUM.value.toString()
                    chatSettings = ChatSettings(
                        notificationSound,
                        vibrationEnabled,
                        videoQuality)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return chatSettings
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
        get() = getChatSettings(db)
        set(chatSettings) {
            setChatSettings(db, chatSettings)
        }

    /**
     * Get chat settings from the current DB.
     *
     * @param db Current DB.
     * @return Chat settings.
     */
    private fun getChatSettings(db: SQLiteDatabase): ChatSettings? {
        Timber.d("getChatSettings")
        var chatSettings: ChatSettings? = null
        val selectQuery = "SELECT * FROM $TABLE_CHAT_SETTINGS"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val notificationSound = decrypt(cursor.getString(2))
                    val vibrationEnabled = decrypt(cursor.getString(3))
                    val videoQuality = decrypt(cursor.getString(4))
                    chatSettings = ChatSettings(
                        notificationSound,
                        vibrationEnabled,
                        videoQuality)
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
    private fun setChatSettings(db: SQLiteDatabase, chatSettings: ChatSettings?) {
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

        db.insert(TABLE_CHAT_SETTINGS, null, values)
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
            return getIntValue(TABLE_CHAT_SETTINGS,
                KEY_CHAT_VIDEO_QUALITY,
                VideoQuality.MEDIUM.value)
        }
        set(chatVideoQuality) {
            Timber.d("setChatVideoQuality")
            setIntValue(TABLE_CHAT_SETTINGS, KEY_CHAT_VIDEO_QUALITY, chatVideoQuality)
        }

    override fun setNotificationSoundChat(sound: String?) {
        val selectQuery = "SELECT * FROM $TABLE_CHAT_SETTINGS"
        val values = ContentValues()
        try {
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_CHAT_SETTINGS SET $KEY_CHAT_SOUND_NOTIFICATIONS= '${
                            encrypt(sound)
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CHAT_SOUND_NOTIFICATIONS, encrypt(sound))
                    db.insert(TABLE_CHAT_SETTINGS, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_CHAT_SETTINGS SET $KEY_CHAT_VIBRATION_ENABLED= '${
                            encrypt(enabled)
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CHAT_VIBRATION_ENABLED, encrypt(enabled))
                    db.insert(TABLE_CHAT_SETTINGS, null, values)
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

        db.insert(TABLE_CHAT_ITEMS, null, values)
    }

    override fun setWrittenTextItem(handle: String?, text: String?, editedMsgId: String?): Int {
        Timber.d("setWrittenTextItem: %s %s", text, handle)
        val values = ContentValues().apply {
            put(KEY_CHAT_ITEM_WRITTEN_TEXT, encrypt(text))
            put(KEY_CHAT_ITEM_EDITED_MSG_ID,
                if (!TextUtil.isTextEmpty(editedMsgId)) encrypt(editedMsgId) else "")
        }

        return db.update(TABLE_CHAT_ITEMS,
            values,
            "$KEY_CHAT_HANDLE = '${encrypt(handle)}'",
            null)
    }

    override fun findChatPreferencesByHandle(handle: String?): ChatItemPreferences? {
        Timber.d("findChatPreferencesByHandle: %s", handle)
        var prefs: ChatItemPreferences?
        val selectQuery =
            "SELECT * FROM $TABLE_CHAT_ITEMS WHERE $KEY_CHAT_HANDLE = '${encrypt(handle)}'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
     * Deletes the oldest completed transfer.
     */
    private fun deleteOldestTransfer() {
        val completedTransfers = completedTransfers
        if (completedTransfers.isNotEmpty()) {
            deleteTransfer((completedTransfers[0] ?: return).id)
        }
    }

    /**
     * Deletes a completed transfer.
     *
     * @param id the identifier of the transfer to delete
     */
    override fun deleteTransfer(id: Long) {
        db.delete(TABLE_COMPLETED_TRANSFERS, "$KEY_ID=$id", null)
    }

    /**
     * Gets a completed transfer.
     *
     * @param id the identifier of the transfer to get
     * @return The completed transfer which has the id value as identifier.
     */
    override fun getCompletedTransfer(id: Long): AndroidCompletedTransfer? {
        val selectQuery =
            "SELECT * FROM $TABLE_COMPLETED_TRANSFERS WHERE $KEY_ID = '$id'"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return extractAndroidCompletedTransfer(cursor)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    /**
     * Extracts a completed transfer of a row.
     *
     * @param cursor Cursor from which the data should be extracted.
     * @return The extracted completed transfer.
     */
    private fun extractAndroidCompletedTransfer(cursor: Cursor): AndroidCompletedTransfer {
        val id = cursor.getString(0).toInt().toLong()
        val filename = decrypt(cursor.getString(1))
        val type = decrypt(cursor.getString(2))
        val typeInt = type!!.toInt()
        val state = decrypt(cursor.getString(3))
        val stateInt = state!!.toInt()
        val size = decrypt(cursor.getString(4))
        val nodeHandle = decrypt(cursor.getString(5))
        val path = decrypt(cursor.getString(6))
        val offline = java.lang.Boolean.parseBoolean(decrypt(cursor.getString(7)))
        val timeStamp = decrypt(cursor.getString(8))!!.toLong()
        val error = decrypt(cursor.getString(9))
        val originalPath = decrypt(cursor.getString(10))
        val parentHandle = decrypt(cursor.getString(11))!!.toLong()
        return AndroidCompletedTransfer(id, filename, typeInt, stateInt, size, nodeHandle, path,
            offline, timeStamp, error, originalPath, parentHandle)
    }

    override fun setCompletedTransfer(transfer: AndroidCompletedTransfer): Long {
        val values = ContentValues().apply {
            put(KEY_TRANSFER_FILENAME, encrypt(transfer.fileName))
            put(KEY_TRANSFER_TYPE, encrypt(transfer.type.toString()))
            put(KEY_TRANSFER_STATE, encrypt(transfer.state.toString()))
            put(KEY_TRANSFER_SIZE, encrypt(transfer.size))
            put(KEY_TRANSFER_HANDLE, encrypt(transfer.nodeHandle))
            put(KEY_TRANSFER_PATH, encrypt(transfer.path))
            put(KEY_TRANSFER_OFFLINE, encrypt(transfer.isOfflineFile.toString()))
            put(KEY_TRANSFER_TIMESTAMP, encrypt(transfer.timeStamp.toString()))
            put(KEY_TRANSFER_ERROR, encrypt(transfer.error))
            put(KEY_TRANSFER_ORIGINAL_PATH, encrypt(transfer.originalPath))
            put(KEY_TRANSFER_PARENT_HANDLE, encrypt(transfer.parentHandle.toString()))
        }

        val id = db.insert(TABLE_COMPLETED_TRANSFERS, null, values)
        if (DatabaseUtils.queryNumEntries(db, TABLE_COMPLETED_TRANSFERS) > MAX_TRANSFERS) {
            deleteOldestTransfer()
        }
        return id
    }

    /**
     * Checks if a completed transfer exists before add it to DB.
     * If so, does nothing. If not, adds the transfer to the DB.
     *
     * @param transfer The transfer to check and add.
     */
    override fun setCompletedTransferWithCheck(transfer: AndroidCompletedTransfer) {
        if (!alreadyExistsAsCompletedTransfer(transfer)) {
            setCompletedTransfer(transfer)
        }
    }

    /**
     * Checks if a completed transfer exists.
     *
     * @param transfer The completed transfer to check.
     * @return True if the transfer already exists, false otherwise.
     */
    private fun alreadyExistsAsCompletedTransfer(transfer: AndroidCompletedTransfer): Boolean {
        val selectQuery = "SELECT * FROM $TABLE_COMPLETED_TRANSFERS " +
                "WHERE $KEY_TRANSFER_FILENAME = '${encrypt(transfer.fileName)}' " +
                "AND $KEY_TRANSFER_TYPE = '${encrypt(transfer.type.toString())}' " +
                "AND $KEY_TRANSFER_STATE = '${encrypt(transfer.state.toString())}' " +
                "AND $KEY_TRANSFER_SIZE = '${encrypt(transfer.size)}' " +
                "AND $KEY_TRANSFER_HANDLE = '${encrypt(transfer.nodeHandle)}' " +
                "AND $KEY_TRANSFER_PATH = '${encrypt(transfer.path)}' " +
                "AND $KEY_TRANSFER_OFFLINE = '${encrypt(transfer.isOfflineFile.toString())}' " +
                "AND $KEY_TRANSFER_ERROR = '${encrypt(transfer.error)}' " +
                "AND $KEY_TRANSFER_ORIGINAL_PATH = '${encrypt(transfer.originalPath)}' " +
                "AND $KEY_TRANSFER_PARENT_HANDLE = '${encrypt(transfer.parentHandle.toString())}'"

        try {
            return db.rawQuery(selectQuery, null)?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return false
    }

    override fun emptyCompletedTransfers() {
        db.delete(TABLE_COMPLETED_TRANSFERS, null, null)
    }

    /**
     * Gets the completed transfers.
     *
     * @return The list with the completed transfers.
     */
    override val completedTransfers: ArrayList<AndroidCompletedTransfer?>
        get() {
            val selectQuery = "SELECT * FROM $TABLE_COMPLETED_TRANSFERS"
            val transfers = getCompletedTransfers(selectQuery)
            transfers.sortWith { transfer1: AndroidCompletedTransfer?, transfer2: AndroidCompletedTransfer? ->
                transfer1!!.timeStamp.compareTo(transfer2!!.timeStamp)
            }
            transfers.reverse()
            return transfers
        }

    /**
     * Gets the completed transfers which have as state cancelled or failed.
     *
     * @return The list the cancelled or failed transfers.
     */
    override val failedOrCancelledTransfers: ArrayList<AndroidCompletedTransfer?>
        get() {
            val stateCancelled = encrypt(MegaTransfer.STATE_CANCELLED.toString())
            val stateFailed = encrypt(MegaTransfer.STATE_FAILED.toString())
            val selectQuery =
                "SELECT * FROM $TABLE_COMPLETED_TRANSFERS WHERE $KEY_TRANSFER_STATE = '$stateCancelled' OR $KEY_TRANSFER_STATE = '$stateFailed' ORDER BY $KEY_TRANSFER_TIMESTAMP DESC"
            return getCompletedTransfers(selectQuery)
        }

    /**
     * Removes the completed transfers which have cancelled or failed as state .
     */
    override fun removeFailedOrCancelledTransfers() {
        val whereCause =
            "$KEY_TRANSFER_STATE = '${encrypt(MegaTransfer.STATE_CANCELLED.toString())}' OR $KEY_TRANSFER_STATE = '${
                encrypt(MegaTransfer.STATE_FAILED.toString())
            }'"
        db.delete(TABLE_COMPLETED_TRANSFERS, whereCause, null)
    }

    /**
     * Gets a list with completed transfers depending on the query received by parameter.
     *
     * @param selectQuery the query which selects specific completed transfers
     * @return The list with the completed transfers.
     */
    override fun getCompletedTransfers(selectQuery: String?): ArrayList<AndroidCompletedTransfer?> {
        val cTs = ArrayList<AndroidCompletedTransfer?>()
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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

    override fun isPasscodeLockEnabled(db: SQLiteDatabase): Boolean {
        Timber.d("getPinLockEnabled")
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        var result = false
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    //get pinLockEnabled
                    decrypt(cursor.getString(7))?.let { pinLockEnabled ->
                        result = pinLockEnabled.toBooleanStrictOrNull() ?: false
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return result
    }

    /**
     * Saves attributes in DB.
     *
     * @param db   DB object to save the attributes.
     * @param attr Attributes to save.
     */
    private fun setAttributes(db: SQLiteDatabase, attr: MegaAttributes?) {
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
        values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP,
            encrypt(attr.extendedAccountDetailsTimeStamp))
        values.put(KEY_INVALIDATE_SDK_CACHE, encrypt(attr.invalidateSdkCache))
        values.put(KEY_USE_HTTPS_ONLY, encrypt(attr.useHttpsOnly))
        values.put(KEY_USE_HTTPS_ONLY, encrypt(attr.useHttpsOnly))
        values.put(KEY_SHOW_COPYRIGHT, encrypt(attr.showCopyright))
        values.put(KEY_SHOW_NOTIF_OFF, encrypt(attr.showNotifOff))
        values.put(KEY_LAST_PUBLIC_HANDLE, encrypt(attr.lastPublicHandle.toString()))
        values.put(KEY_LAST_PUBLIC_HANDLE_TIMESTAMP,
            encrypt(attr.lastPublicHandleTimeStamp.toString()))
        values.put(KEY_STORAGE_STATE,
            encrypt(storageStateIntMapper(attr.storageState).toString()))
        values.put(KEY_LAST_PUBLIC_HANDLE_TYPE,
            encrypt(attr.lastPublicHandleType.toString()))
        values.put(KEY_MY_CHAT_FILES_FOLDER_HANDLE,
            encrypt(attr.myChatFilesFolderHandle.toString()))
        values.put(KEY_TRANSFER_QUEUE_STATUS, encrypt(attr.transferQueueStatus))
        db.insert(TABLE_ATTRIBUTES, null, values)
    }

    /**
     * Gets attributes.
     *
     * @param db Current DB.
     * @return The attributes.
     */
    private fun getAttributes(db: SQLiteDatabase): MegaAttributes? {
        var attr: MegaAttributes? = null
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val online = decrypt(cursor.getString(getColumnIndex(cursor, KEY_ATTR_ONLINE)))
                    val intents =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_ATTR_INTENTS)))
                    val askSizeDownload = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_ATTR_ASK_SIZE_DOWNLOAD)))
                    val askNoAppDownload = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_ATTR_ASK_NOAPP_DOWNLOAD)))
                    if (!legacyLoggingSettings.areSDKLogsEnabled() && cursor.getColumnIndex(
                            KEY_FILE_LOGGER_SDK) != Constants.INVALID_VALUE
                    ) {
                        val fileLoggerSDK =
                            decrypt(cursor.getString(getColumnIndex(cursor, KEY_FILE_LOGGER_SDK)))
                        legacyLoggingSettings.updateSDKLogs(java.lang.Boolean.parseBoolean(
                            fileLoggerSDK))
                    }
                    val accountDetailsTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_ACCOUNT_DETAILS_TIMESTAMP)))
                    val extendedAccountDetailsTimeStamp = decrypt(cursor.getString(getColumnIndex(
                        cursor,
                        KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP)))
                    val invalidateSdkCache =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_INVALIDATE_SDK_CACHE)))
                    if (!legacyLoggingSettings.areKarereLogsEnabled() && cursor.getColumnIndex(
                            KEY_FILE_LOGGER_KARERE) != Constants.INVALID_VALUE
                    ) {
                        val fileLoggerKarere = decrypt(cursor.getString(getColumnIndex(cursor,
                            KEY_FILE_LOGGER_KARERE)))
                        legacyLoggingSettings.updateKarereLogs(java.lang.Boolean.parseBoolean(
                            fileLoggerKarere))
                    }
                    val useHttpsOnly =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_USE_HTTPS_ONLY)))
                    val showCopyright =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_COPYRIGHT)))
                    val showNotifOff =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_SHOW_NOTIF_OFF)))
                    val lastPublicHandle =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_LAST_PUBLIC_HANDLE)))
                    val lastPublicHandleTimeStamp = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_LAST_PUBLIC_HANDLE_TIMESTAMP)))
                    val storageState =
                        decrypt(cursor.getString(getColumnIndex(cursor, KEY_STORAGE_STATE)))
                    val lastPublicHandleType = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_LAST_PUBLIC_HANDLE_TYPE)))
                    val myChatFilesFolderHandle = decrypt(cursor.getString(getColumnIndex(cursor,
                        KEY_MY_CHAT_FILES_FOLDER_HANDLE)))
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
        get() = getAttributes(db)
        set(attr) {
            setAttributes(db, attr)
        }

    override fun setNonContactFirstName(name: String?, handle: String?): Int {
        Timber.d("setContactName: %s %s", name, handle)
        val values = ContentValues().apply {
            put(KEY_NONCONTACT_FIRSTNAME, encrypt(name))
        }
        val rows = db.update(TABLE_NON_CONTACTS,
            values,
            "$KEY_NONCONTACT_HANDLE = '${encrypt(handle)}'",
            null)
        if (rows == 0) {
            values.put(KEY_NONCONTACT_HANDLE, encrypt(handle))
            db.insert(TABLE_NON_CONTACTS, null, values)
        }
        return rows
    }

    override fun setNonContactLastName(lastName: String?, handle: String?): Int {
        val values = ContentValues().apply {
            put(KEY_NONCONTACT_LASTNAME, encrypt(lastName))
        }
        val rows = db.update(TABLE_NON_CONTACTS,
            values,
            "$KEY_NONCONTACT_HANDLE = '${encrypt(handle)}'",
            null)
        if (rows == 0) {
            values.put(KEY_NONCONTACT_HANDLE, encrypt(handle))
            db.insert(TABLE_NON_CONTACTS, null, values)
        }
        return rows
    }

    override fun setNonContactEmail(email: String?, handle: String?): Int {
        val values = ContentValues().apply {
            put(KEY_NONCONTACT_EMAIL, encrypt(email))
        }
        val rows = db.update(TABLE_NON_CONTACTS,
            values,
            "$KEY_NONCONTACT_HANDLE = '${encrypt(handle)}'",
            null)
        if (rows == 0) {
            values.put(KEY_NONCONTACT_HANDLE, encrypt(handle))
            db.insert(TABLE_NON_CONTACTS, null, values)
        }
        return rows
    }

    override fun findNonContactByHandle(handle: String?): NonContactInfo? {
        Timber.d("findNONContactByHandle: %s", handle)
        val selectQuery =
            "SELECT * FROM $TABLE_NON_CONTACTS WHERE $KEY_NONCONTACT_HANDLE = '${encrypt(handle)}'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                        email)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun setContact(contact: MegaContactDB) {
        val values = ContentValues().apply {
            put(KEY_CONTACT_HANDLE, encrypt(contact.getHandle()))
            put(KEY_CONTACT_MAIL, encrypt(contact.getMail()))
            put(KEY_CONTACT_NAME, encrypt(contact.getName()))
            put(KEY_CONTACT_LAST_NAME, encrypt(contact.getLastName()))
            put(KEY_CONTACT_NICKNAME, encrypt(contact.getNickname()))
        }
        db.insert(TABLE_CONTACTS, null, values)
    }

    override fun setContactName(name: String?, mail: String?): Int {
        val values = ContentValues().apply {
            put(KEY_CONTACT_NAME, encrypt(name))
        }
        return db.update(TABLE_CONTACTS,
            values,
            KEY_CONTACT_MAIL + " = '" + encrypt(mail) + "'",
            null)
    }

    override fun setContactLastName(lastName: String?, mail: String?): Int {
        val values = ContentValues().apply {
            put(KEY_CONTACT_LAST_NAME, encrypt(lastName))
        }
        return db.update(TABLE_CONTACTS,
            values,
            "$KEY_CONTACT_MAIL = '${encrypt(mail)}'",
            null)
    }

    override fun setContactNickname(nickname: String?, handle: Long): Int {
        val values = ContentValues().apply {
            put(KEY_CONTACT_NICKNAME, encrypt(nickname))
        }
        return db.update(TABLE_CONTACTS,
            values,
            "$KEY_CONTACT_HANDLE = '${encrypt(handle.toString())}'",
            null)
    }

    override val contactsSize: Int
        get() {
            val selectQuery = "SELECT * FROM $TABLE_CONTACTS"
            try {
                return db.rawQuery(selectQuery, null)?.count ?: 0
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return 0
        }

    override fun setContactMail(handle: Long, mail: String?): Int {
        Timber.d("setContactMail: %d %s", handle, mail)
        val values = ContentValues().apply {
            put(KEY_CONTACT_MAIL, encrypt(mail))
        }
        return db.update(TABLE_CONTACTS,
            values,
            "$KEY_CONTACT_HANDLE = '${encrypt(handle.toString())}'",
            null)
    }

    override fun setContactFistName(handle: Long, firstName: String?): Int {
        Timber.d("setContactFistName: %d %s", handle, firstName)
        val values = ContentValues().apply {
            put(KEY_CONTACT_NAME, encrypt(firstName))
        }
        return db.update(TABLE_CONTACTS,
            values,
            "$KEY_CONTACT_HANDLE = '${encrypt(handle.toString())}'",
            null)
    }

    override fun setContactLastName(handle: Long, lastName: String?): Int {
        Timber.d("setContactLastName: %d %s", handle, lastName)
        val values = ContentValues().apply {
            put(KEY_CONTACT_LAST_NAME, encrypt(lastName))
        }
        return db.update(TABLE_CONTACTS,
            values,
            "$KEY_CONTACT_HANDLE = '${encrypt(handle.toString())}'",
            null)
    }

    override fun findContactByHandle(handleParam: String?): MegaContactDB? {
        Timber.d("findContactByHandle: %s", handleParam)
        val selectQuery =
            "SELECT * FROM $TABLE_CONTACTS WHERE $KEY_CONTACT_HANDLE = '${encrypt(handleParam)}'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val handle = decrypt(cursor.getString(1))
                    val mail = decrypt(cursor.getString(2))
                    val name = decrypt(cursor.getString(3))
                    val lastName = decrypt(cursor.getString(4))
                    val nickname = decrypt(cursor.getString(5))
                    return MegaContactDB(handle,
                        mail,
                        name,
                        lastName,
                        nickname)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun findContactByEmail(mail: String?): MegaContactDB? {
        Timber.d("findContactByEmail: %s", mail)
        val selectQuery =
            "SELECT * FROM $TABLE_CONTACTS WHERE $KEY_CONTACT_MAIL = '${encrypt(mail)}'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val handle = decrypt(cursor.getString(1))
                    val name = decrypt(cursor.getString(3))
                    val lastName = decrypt(cursor.getString(4))
                    val nickname = decrypt(cursor.getString(5))
                    return MegaContactDB(handle,
                        mail,
                        name,
                        lastName,
                        nickname)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun setOfflineFile(offline: MegaOffline): Long {
        Timber.d("setOfflineFile: %s", offline.handle)
        val values = ContentValues()
        val checkInsert: MegaOffline? = findByHandle(offline.handle)
        if (checkInsert == null) {
            val nullColumnHack: String? = null
            values.put(KEY_OFF_HANDLE, encrypt(offline.handle))
            values.put(KEY_OFF_PATH, encrypt(offline.path))
            values.put(KEY_OFF_NAME, encrypt(offline.name))
            values.put(KEY_OFF_PARENT, offline.parentId)
            values.put(KEY_OFF_TYPE, encrypt(offline.type))
            values.put(KEY_OFF_INCOMING, offline.origin)
            values.put(KEY_OFF_HANDLE_INCOMING, encrypt(offline.handleIncoming))
            return db.insert(TABLE_OFFLINE, nullColumnHack, values)
        }
        return -1
    }

    override fun setOfflineFile(offline: MegaOffline, db: SQLiteDatabase): Long {
        val values = ContentValues()
        val checkInsert: MegaOffline? = findByHandle(offline.handle)
        if (checkInsert == null) {
            val nullColumnHack: String? = null
            values.put(KEY_OFF_HANDLE, encrypt(offline.handle))
            values.put(KEY_OFF_PATH, encrypt(offline.path))
            values.put(KEY_OFF_NAME, encrypt(offline.name))
            values.put(KEY_OFF_PARENT, offline.parentId)
            values.put(KEY_OFF_TYPE, encrypt(offline.type))
            values.put(KEY_OFF_INCOMING, offline.origin)
            values.put(KEY_OFF_HANDLE_INCOMING, encrypt(offline.handleIncoming))
            return db.insert(TABLE_OFFLINE, nullColumnHack, values)
        }
        return -1
    }

    override fun setOfflineFileOld(offline: MegaOffline): Long {
        val values = ContentValues()
        val checkInsert: MegaOffline? = findByHandle(offline.handle)
        if (checkInsert == null) {
            val nullColumnHack: String? = null
            values.put(KEY_OFF_HANDLE, offline.handle)
            values.put(KEY_OFF_PATH, offline.path)
            values.put(KEY_OFF_NAME, offline.name)
            values.put(KEY_OFF_PARENT, offline.parentId)
            values.put(KEY_OFF_TYPE, offline.type)
            values.put(KEY_OFF_INCOMING, offline.origin)
            values.put(KEY_OFF_HANDLE_INCOMING, offline.handleIncoming)
            return db.insert(TABLE_OFFLINE, nullColumnHack, values)
        }
        return -1
    }

    override fun setOfflineFileOld(offline: MegaOffline, db: SQLiteDatabase): Long {
        val values = ContentValues()
        val checkInsert: MegaOffline? = findByHandle(offline.handle)
        if (checkInsert == null) {
            val nullColumnHack: String? = null
            values.put(KEY_OFF_HANDLE, offline.handle)
            values.put(KEY_OFF_PATH, offline.path)
            values.put(KEY_OFF_NAME, offline.name)
            values.put(KEY_OFF_PARENT, offline.parentId)
            values.put(KEY_OFF_TYPE, offline.type)
            values.put(KEY_OFF_INCOMING, offline.origin)
            values.put(KEY_OFF_HANDLE_INCOMING, offline.handleIncoming)
            return db.insert(TABLE_OFFLINE, nullColumnHack, values)
        }
        return -1
    }

    override val offlineFiles: ArrayList<MegaOffline>
        get() {
            val listOffline = ArrayList<MegaOffline>()
            val selectQuery = "SELECT * FROM $TABLE_OFFLINE"
            try {
                db.rawQuery(selectQuery, null)?.use { cursor ->
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
                            val offline = MegaOffline(id,
                                handle.toString(),
                                path.toString(),
                                name.toString(),
                                parent,
                                type,
                                incoming,
                                handleIncoming.toString())
                            listOffline.add(offline)
                        } while (cursor.moveToNext())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return listOffline
        }

    override fun getOfflineFilesOld(db: SQLiteDatabase): ArrayList<MegaOffline> {
        val listOffline = ArrayList<MegaOffline>()
        val selectQuery = "SELECT * FROM $TABLE_OFFLINE"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                        val offline = MegaOffline(id,
                            handle,
                            path,
                            name,
                            parent,
                            type,
                            incoming,
                            handleIncoming)
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
            return db.rawQuery(selectQuery, null)?.use { it.moveToFirst() } ?: false
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
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(0).toInt()
                    val nodeHandle = decrypt(cursor.getString(1))
                    val path = decrypt(cursor.getString(2))
                    val name = decrypt(cursor.getString(3))
                    val parent = cursor.getInt(4)
                    val type = decrypt(cursor.getString(5))
                    val incoming = cursor.getInt(6)
                    val handleIncoming = decrypt(cursor.getString(7))
                    return MegaOffline(id,
                        nodeHandle.toString(),
                        path.toString(),
                        name.toString(),
                        parent,
                        type,
                        incoming,
                        handleIncoming.toString())
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
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                        listOffline.add(MegaOffline(_id,
                            _handle.toString(),
                            _path.toString(),
                            _name.toString(),
                            _parent,
                            _type,
                            _incoming,
                            _handleIncoming.toString()))
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
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                        offline = MegaOffline(_id,
                            _handle.toString(),
                            _path.toString(),
                            _name.toString(),
                            _parent,
                            _type,
                            _incoming,
                            _handleIncoming.toString())
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return offline
    }

    override fun removeById(id: Int): Int {
        return db.delete(TABLE_OFFLINE, "$KEY_ID=$id", null)
    }

    override fun findByPath(path: String?): ArrayList<MegaOffline> {
        val listOffline = ArrayList<MegaOffline>()
        //Get the foreign key of the node
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_PATH = '${encrypt(path)}'"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                        listOffline.add(MegaOffline(_id,
                            _handle.toString(),
                            _path.toString(),
                            _name.toString(),
                            _parent,
                            _type,
                            _incoming,
                            _handleIncoming.toString()))
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
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
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                        offline = MegaOffline(_id,
                            _handle.toString(),
                            _path.toString(),
                            _name.toString(),
                            _parent,
                            _type,
                            _incoming,
                            _handleIncoming.toString())
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return offline
    }

    override fun deleteOfflineFile(mOff: MegaOffline): Int {
        return this.writableDatabase.delete(TABLE_OFFLINE,
            "$KEY_OFF_HANDLE = ?",
            arrayOf(encrypt(mOff.handle.toString())))
    }

    override fun setFirstTime(firstTime: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_FIRST_LOGIN= '${encrypt(firstTime.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_FIRST_LOGIN, encrypt(firstTime.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_WIFI= '${encrypt(wifi.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_WIFI, encrypt(wifi.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_VIEW_LIST= '${encrypt(list.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_PREFERRED_VIEW_LIST, encrypt(list.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_VIEW_LIST_CAMERA= '${
                            encrypt(list.toString())
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_PREFERRED_VIEW_LIST_CAMERA, encrypt(list.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_SORT_CLOUD= '${encrypt(order)}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_PREFERRED_SORT_CLOUD, encrypt(order))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_PREFERRED_SORT_OTHERS= '${encrypt(order)}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_PREFERRED_SORT_OTHERS, encrypt(order))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_LAST_UPLOAD_FOLDER= '${
                            encrypt(folderPath)
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_LAST_UPLOAD_FOLDER, encrypt(folderPath))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_LAST_CLOUD_FOLDER_HANDLE= '${
                            encrypt(folderHandle)
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                    Timber.d("KEY_LAST_CLOUD_FOLDER_HANDLE UPLOAD FOLDER: %s",
                        UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_LAST_CLOUD_FOLDER_HANDLE, encrypt(folderHandle))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_KEEP_FILE_NAMES= '${encrypt(charging.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_KEEP_FILE_NAMES, encrypt(charging.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_ENABLED= '${encrypt(enabled.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_ENABLED, encrypt(enabled.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
                }
                if (enabled) {
                    setPrimaryBackup()
                } else {
                    removePrimaryBackup()
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_ENABLED= '${encrypt(enabled.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_SEC_FOLDER_ENABLED, encrypt(enabled.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
                }
                // Set or remove corresponding MU backup.
                if (enabled) {
                    setSecondaryBackup()
                } else {
                    removeSecondaryBackup()
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_HANDLE= '${encrypt(handle.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_HANDLE, encrypt(handle.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
                }
                Timber.d("Set new primary handle: %s", handle)
                // Update CU backup when CU target folder changed.
                updatePrimaryFolderTargetNode(handle)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_HANDLE= '${encrypt(handle.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_SEC_FOLDER_HANDLE, encrypt(handle.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
                }
                Timber.d("Set new secondary handle: %s", handle)
                // Update MU backup when MU target folder changed.
                updateSecondaryFolderTargetNode(handle)
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun setCamSyncLocalPath(localPath: String) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_LOCAL_PATH= '${
                            encrypt(localPath)
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_LOCAL_PATH, encrypt(localPath))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_URI_EXTERNAL_SD_CARD= '${
                            encrypt(uriExternalSDCard)
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                    Timber.d("KEY_URI_EXTERNAL_SD_CARD URI: %s", UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_URI_EXTERNAL_SD_CARD, encrypt(uriExternalSDCard))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            setStringValue(TABLE_PREFERENCES,
                KEY_URI_MEDIA_EXTERNAL_SD_CARD,
                uriMediaExternalSdCard)
        }

    override fun setCameraFolderExternalSDCard(cameraFolderExternalSDCard: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD= '${
                            encrypt(cameraFolderExternalSDCard.toString())
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD,
                        encrypt(cameraFolderExternalSDCard.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            setStringValue(TABLE_PREFERENCES,
                KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD,
                mediaFolderExternalSdCard.toString())
        }

    override var passcodeLockType: String?
        get() = getStringValue(TABLE_PREFERENCES, KEY_PASSCODE_LOCK_TYPE, "")
        set(passcodeLockType) {
            Timber.d("setPasscodeLockType")
            val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
            val values = ContentValues()
            try {
                db.rawQuery(selectQuery, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val UPDATE_PREFERENCES_TABLE =
                            "UPDATE $TABLE_PREFERENCES SET $KEY_PASSCODE_LOCK_TYPE= '${
                                encrypt(passcodeLockType)
                            }' WHERE $KEY_ID = '1'"
                        db.execSQL(UPDATE_PREFERENCES_TABLE)
                    } else {
                        values.put(KEY_PASSCODE_LOCK_TYPE, encrypt(passcodeLockType))
                        db.insert(TABLE_PREFERENCES, null, values)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
        }

    override fun setSecondaryFolderPath(localPath: String) {
        Timber.d("setSecondaryFolderPath: %s", localPath)
        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_SEC_FOLDER_LOCAL_PATH= '${
                            encrypt(localPath)
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_SEC_FOLDER_LOCAL_PATH, encrypt(localPath))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_CAM_SYNC_FILE_UPLOAD= '${
                            encrypt(fileUpload.toString())
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_CAM_SYNC_FILE_UPLOAD, encrypt(fileUpload.toString()))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTE_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_ACCOUNT_DETAILS_TIMESTAMP= '${
                            encrypt(accountDetailsTimeStamp.toString())
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_ATTRIBUTE_TABLE)
                } else {
                    values.put(KEY_ACCOUNT_DETAILS_TIMESTAMP,
                        encrypt(accountDetailsTimeStamp.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTE_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP= '${
                            encrypt(extendedAccountDetailsTimestamp.toString())
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_ATTRIBUTE_TABLE)
                } else {
                    values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP,
                        encrypt(extendedAccountDetailsTimestamp.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTE_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP= '${
                            encrypt(extendedAccountDetailsTimestamp.toString())
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_ATTRIBUTE_TABLE)
                } else {
                    values.put(KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP,
                        encrypt(extendedAccountDetailsTimestamp.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_TABLE =
                        "UPDATE $tableName SET $columnName= '${encrypt(value)}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_TABLE)
                } else {
                    val values = ContentValues()
                    values.put(columnName, encrypt(value))
                    db.insert(tableName, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    value = decrypt(cursor.getString(0))
                    Timber.d("%s value: %s", columnName, value)
                } else {
                    Timber.w("No value found, setting default")
                    val values = ContentValues()
                    values.put(columnName, encrypt(defaultValue))
                    db.insert(tableName, null, values)
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
                db.rawQuery(selectQuery, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val UPDATE_PREFERENCES_TABLE =
                            "UPDATE $TABLE_PREFERENCES SET $KEY_PASSCODE_LOCK_ENABLED= '${
                                encrypt(passcodeLockEnabled.toString())
                            }' WHERE $KEY_ID = '1'"
                        db.execSQL(UPDATE_PREFERENCES_TABLE)
                    } else {
                        values.put(KEY_PASSCODE_LOCK_ENABLED,
                            encrypt(passcodeLockEnabled.toString()))
                        db.insert(TABLE_PREFERENCES, null, values)
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
                db.rawQuery(selectQuery, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val UPDATE_PREFERENCES_TABLE =
                            "UPDATE $TABLE_PREFERENCES SET $KEY_PASSCODE_LOCK_CODE= '${
                                encrypt(
                                    passcodeLockCode)
                            }' WHERE $KEY_ID = '1'"
                        db.execSQL(UPDATE_PREFERENCES_TABLE)
                    } else {
                        values.put(KEY_PASSCODE_LOCK_CODE, encrypt(passcodeLockCode))
                        db.insert(TABLE_PREFERENCES, null, values)
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
        get() = getStringValue(TABLE_PREFERENCES,
            KEY_PASSCODE_LOCK_REQUIRE_TIME,
            Constants.REQUIRE_PASSCODE_INVALID.toString())?.toIntOrNull()
            ?: Constants.REQUIRE_PASSCODE_INVALID
        set(requiredTime) {
            setStringValue(TABLE_PREFERENCES,
                KEY_PASSCODE_LOCK_REQUIRE_TIME,
                requiredTime.toString())
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
            setStringValue(TABLE_PREFERENCES,
                KEY_ASK_SET_DOWNLOAD_LOCATION,
                askSetDownloadLocation.toString())
        }

    override fun setStorageDownloadLocation(storageDownloadLocation: String?) {
        if (storageDownloadLocation == null) return

        val selectQuery = "SELECT * FROM $TABLE_PREFERENCES"
        val values = ContentValues()
        try {
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_PREFERENCES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_STORAGE_DOWNLOAD_LOCATION= '${
                            encrypt(storageDownloadLocation)
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_PREFERENCES_TABLE)
                } else {
                    values.put(KEY_STORAGE_DOWNLOAD_LOCATION, encrypt(storageDownloadLocation))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_ASK_SIZE_DOWNLOAD='${
                            encrypt(askSizeDownload)
                        }' WHERE $KEY_ID ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_ATTR_ASK_SIZE_DOWNLOAD, encrypt(askSizeDownload))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_ASK_NOAPP_DOWNLOAD='${
                            encrypt(askNoAppDownload)
                        }' WHERE $KEY_ID ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_ATTR_ASK_NOAPP_DOWNLOAD, encrypt(askNoAppDownload))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_ATTR_INTENTS='${
                            encrypt(attempt.toString())
                        }' WHERE $KEY_ID ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_ATTR_INTENTS, encrypt(attempt.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_USE_HTTPS_ONLY='${encrypt(useHttpsOnly.toString())}' WHERE $KEY_ID ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_USE_HTTPS_ONLY, encrypt(useHttpsOnly.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
                db.rawQuery(selectQuery, null)?.use { cursor ->
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_SHOW_COPYRIGHT='${encrypt(showCopyright.toString())}' WHERE $KEY_ID ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_SHOW_COPYRIGHT, encrypt(showCopyright.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override val showCopyright: String?
        get() {
            val selectQuery =
                "SELECT $KEY_SHOW_COPYRIGHT FROM $TABLE_ATTRIBUTES WHERE $KEY_ID = '1'"
            try {
                db.rawQuery(selectQuery, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return decrypt(cursor.getString(0))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return "true"
        }

    override fun setShowNotifOff(showNotifOff: Boolean) {
        val selectQuery = "SELECT * FROM $TABLE_ATTRIBUTES"
        val values = ContentValues()
        try {
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_SHOW_NOTIF_OFF='${encrypt(showNotifOff.toString())}' WHERE $KEY_ID ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_SHOW_NOTIF_OFF, encrypt(showNotifOff.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_LAST_PUBLIC_HANDLE= '${encrypt(handle.toString())}' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_LAST_PUBLIC_HANDLE, encrypt(handle.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTE_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_LAST_PUBLIC_HANDLE_TIMESTAMP= '${
                            encrypt(lastPublicHandleTimeStamp.toString())
                        }' WHERE $KEY_ID = '1'"
                    db.execSQL(UPDATE_ATTRIBUTE_TABLE)
                } else {
                    values.put(KEY_LAST_PUBLIC_HANDLE_TIMESTAMP,
                        encrypt(lastPublicHandleTimeStamp.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
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
            return getIntValue(TABLE_ATTRIBUTES,
                KEY_LAST_PUBLIC_HANDLE_TYPE,
                MegaApiJava.AFFILIATE_TYPE_INVALID)
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
            return getLongValue(TABLE_ATTRIBUTES,
                KEY_MY_CHAT_FILES_FOLDER_HANDLE,
                MegaApiJava.INVALID_HANDLE)
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
            setStringValue(TABLE_ATTRIBUTES,
                KEY_TRANSFER_QUEUE_STATUS,
                transferQueueStatus.toString())
        }

    override val showNotifOff: String?
        get() {
            val selectQuery =
                "SELECT $KEY_SHOW_NOTIF_OFF FROM $TABLE_ATTRIBUTES WHERE $KEY_ID = '1'"
            try {
                db.rawQuery(selectQuery, null)?.use { cursor ->
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_ATTRIBUTES SET $KEY_INVALIDATE_SDK_CACHE='" + encrypt(
                            invalidateSdkCache.toString()) + "' WHERE " + KEY_ID + " ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                    Timber.d("UPDATE_ATTRIBUTES_TABLE : %s", UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_INVALIDATE_SDK_CACHE,
                        encrypt(invalidateSdkCache.toString()))
                    db.insert(TABLE_ATTRIBUTES, null, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override fun clearCredentials() {
        Timber.w("Clear local credentials!")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CREDENTIALS")
        onCreate(db)
    }

    override fun clearEphemeral() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EPHEMERAL")
        onCreate(db)
    }

    override fun clearPreferences() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PREFERENCES")
        onCreate(db)
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
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTRIBUTES")
        onCreate(db)
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
        db.execSQL("DELETE FROM $TABLE_CONTACTS")
    }

    override fun clearNonContacts() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NON_CONTACTS")
        onCreate(db)
    }

    override fun clearChatItems() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_ITEMS")
        onCreate(db)
    }

    override fun clearChatSettings() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_SETTINGS")
        onCreate(db)
    }

    override fun clearOffline(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OFFLINE")
        onCreate(db)
    }

    override fun clearOffline() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OFFLINE")
        onCreate(db)
    }

    override fun clearCompletedTransfers() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMPLETED_TRANSFERS")
        onCreate(db)
    }

    override fun clearPendingMessage() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_MSG")
        onCreate(db)
    }

    /**
     * Adds a pending message from File Explorer.
     *
     * @param message Pending message to add.
     * @return The identifier of the pending message.
     */
    override fun addPendingMessageFromFileExplorer(message: PendingMessageSingle): Long {
        return addPendingMessage(message, PendingMessageSingle.STATE_PREPARING_FROM_EXPLORER)
    }

    /**
     * Adds a pending message.
     *
     * @param message Pending message to add.
     * @param state   State of the pending message.
     * @return The identifier of the pending message.
     */
    override fun addPendingMessage(message: PendingMessageSingle): Long {
        return addPendingMessage(message, PendingMessageSingle.STATE_PREPARING)
    }

    /**
     * Adds a pending message.
     *
     * @param message Pending message to add.
     * @return The identifier of the pending message.
     */
    override fun addPendingMessage(
        message: PendingMessageSingle,
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
        return db.insert(TABLE_PENDING_MSG_SINGLE, null, values)
    }

    override fun findPendingMessageById(messageId: Long): PendingMessageSingle? {
        Timber.d("findPendingMessageById")
        var pendMsg: PendingMessageSingle? = null
        val selectQuery =
            "SELECT * FROM $TABLE_PENDING_MSG_SINGLE WHERE $KEY_ID ='$messageId'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                    pendMsg = PendingMessageSingle(messageId,
                        chatId,
                        timestamp,
                        idTempKarere,
                        filePath,
                        fingerPrint,
                        name,
                        nodeHandle,
                        transferTag,
                        state)
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
        updatePendingMessage(idMessage,
            transferTag,
            Constants.INVALID_OPTION,
            PendingMessageSingle.STATE_UPLOADING)
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
        db.update(TABLE_PENDING_MSG_SINGLE, values, where, null)
    }

    override fun updatePendingMessageOnAttach(idMessage: Long, temporalId: String?, state: Int) {
        val values = ContentValues()
        Timber.d("ID of my pending message to update: %s", temporalId)
        values.put(KEY_PENDING_MSG_TEMP_KARERE, encrypt(temporalId))
        values.put(KEY_PENDING_MSG_STATE, state)
        val where = "$KEY_ID=$idMessage"
        val rows = db.update(TABLE_PENDING_MSG_SINGLE, values, where, null)
        Timber.d("Rows updated: %s", rows)
    }

    override fun findPendingMessagesNotSent(idChat: Long): ArrayList<AndroidMegaChatMessage> {
        Timber.d("findPendingMessagesNotSent")
        val pendMsgs = ArrayList<AndroidMegaChatMessage>()
        val chat = idChat.toString()
        val selectQuery =
            "SELECT * FROM $TABLE_PENDING_MSG_SINGLE WHERE $KEY_PENDING_MSG_STATE < ${PendingMessageSingle.STATE_SENT} AND $KEY_ID_CHAT ='${
                encrypt(chat)
            }'"
        Timber.d("QUERY: %s", selectQuery)
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                        val pendMsg = PendingMessageSingle(id,
                            chatId,
                            timestamp,
                            idTempKarere,
                            filePath,
                            fingerPrint,
                            name,
                            nodeHandle,
                            transferTag,
                            state)
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
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
        db.delete(TABLE_PENDING_MSG_SINGLE,
            KEY_PENDING_MSG_STATE + "=" + PendingMessageSingle.STATE_SENT,
            null)
    }

    override fun removePendingMessageByChatId(idChat: Long) {
        Timber.d("removePendingMessageByChatId")
        db.delete(TABLE_PENDING_MSG_SINGLE,
            "$KEY_PENDING_MSG_ID_CHAT = '${encrypt(idChat.toString())}'",
            null)
    }

    override fun removePendingMessageById(idMsg: Long) {
        db.delete(TABLE_PENDING_MSG_SINGLE, "$KEY_ID=$idMsg", null)
    }

    override val autoPlayEnabled: String?
        get() {
            val selectQuery =
                "SELECT $KEY_AUTO_PLAY FROM $TABLE_PREFERENCES WHERE $KEY_ID = '1'"
            try {
                db.rawQuery(selectQuery, null)?.use { cursor ->
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_AUTO_PLAY='${encrypt(enabled)}' WHERE $KEY_ID ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_AUTO_PLAY, encrypt(enabled))
                    db.insert(TABLE_PREFERENCES, null, values)
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
            db.rawQuery(selectQuery, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val UPDATE_ATTRIBUTES_TABLE =
                        "UPDATE $TABLE_PREFERENCES SET $KEY_SHOW_INVITE_BANNER='${encrypt(show)}' WHERE $KEY_ID ='1'"
                    db.execSQL(UPDATE_ATTRIBUTES_TABLE)
                } else {
                    values.put(KEY_SHOW_INVITE_BANNER, encrypt(show))
                    db.insert(TABLE_PREFERENCES, null, values)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
    }

    override val sdTransfers: ArrayList<SDTransfer>
        get() {
            val sdTransfers = ArrayList<SDTransfer>()
            val selectQuery = "SELECT * FROM $TABLE_SD_TRANSFERS"
            try {
                db.rawQuery(selectQuery, null)?.use { cursor ->
                    if (cursor.moveToLast()) {
                        do {
                            val tag = cursor.getString(1).toInt()
                            val name = decrypt(cursor.getString(2))
                            val size = decrypt(cursor.getString(3))
                            val nodeHandle = decrypt(cursor.getString(4))
                            val path = decrypt(cursor.getString(5))
                            val appData = decrypt(cursor.getString(6))
                            sdTransfers.add(SDTransfer(tag,
                                name!!,
                                size!!,
                                nodeHandle!!,
                                path!!,
                                appData!!))
                        } while (cursor.moveToPrevious())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return sdTransfers
        }

    override fun addSDTransfer(transfer: SDTransfer): Long {
        val values = ContentValues()
        values.put(KEY_SD_TRANSFERS_TAG, transfer.tag)
        values.put(KEY_SD_TRANSFERS_NAME, encrypt(transfer.name))
        values.put(KEY_SD_TRANSFERS_SIZE, encrypt(transfer.size))
        values.put(KEY_SD_TRANSFERS_HANDLE, encrypt(transfer.nodeHandle))
        values.put(KEY_SD_TRANSFERS_PATH, encrypt(transfer.path))
        values.put(KEY_SD_TRANSFERS_APP_DATA, encrypt(transfer.appData))
        return db.insert(TABLE_SD_TRANSFERS, null, values)
    }

    override fun removeSDTransfer(tag: Int) {
        db.delete(TABLE_SD_TRANSFERS,
            "$KEY_SD_TRANSFERS_TAG=$tag",
            null)
    }

    override fun saveBackup(backup: Backup): Boolean {
        val values = ContentValues()
        values.put(KEY_BACKUP_ID, encrypt(backup.backupId.toString()))
        values.put(KEY_BACKUP_TYPE, backup.backupType)
        values.put(KEY_BACKUP_TARGET_NODE, encrypt(backup.targetNode.toString()))
        values.put(KEY_BACKUP_LOCAL_FOLDER, encrypt(backup.localFolder))
        values.put(KEY_BACKUP_NAME, encrypt(backup.backupName))
        values.put(KEY_BACKUP_STATE, backup.state.value)
        values.put(KEY_BACKUP_SUB_STATE, backup.subState)
        values.put(KEY_BACKUP_EXTRA_DATA, encrypt(backup.extraData))
        values.put(KEY_BACKUP_START_TIME, encrypt(backup.startTimestamp.toString()))
        values.put(KEY_BACKUP_LAST_TIME,
            encrypt(backup.lastFinishTimestamp.toString()))
        values.put(KEY_BACKUP_TARGET_NODE_PATH, encrypt(backup.targetFolderPath))
        values.put(KEY_BACKUP_EX, encrypt(backup.isExcludeSubFolders.toString()))
        values.put(KEY_BACKUP_DEL,
            encrypt(java.lang.Boolean.toString(backup.isDeleteEmptySubFolders)))
        // Default value is false.
        values.put(KEY_BACKUP_OUTDATED, encrypt("false"))
        val result = db.insertOrThrow(TABLE_BACKUPS, null, values)
        return if (result != -1L) {
            Timber.d("Save sync pair %s successfully, row id is: %d", backup, result)
            true
        } else {
            Timber.e("Save sync pair %s failed", backup)
            false
        }
    }

    override val cuBackupID: Long?
        get() = cuBackup?.backupId

    override val muBackupID: Long?
        get() = muBackup?.backupId

    override val cuBackup: Backup?
        get() = getBackupByType(MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS)
    override val muBackup: Backup?
        get() = getBackupByType(MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS)

    private fun getBackupByType(type: Int): Backup? {
        val selectQuery =
            "SELECT * FROM $TABLE_BACKUPS WHERE $KEY_BACKUP_TYPE = $type AND $KEY_BACKUP_OUTDATED = '${
                encrypt("false")
            }' ORDER BY $KEY_ID DESC"

        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return getBackupFromCursor(cursor)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override fun setBackupAsOutdated(id: Long) {
        getBackupById(id)?.let { backup ->
            updateBackup(backup.copy(outdated = true))
        }
    }

    override fun getBackupById(id: Long): Backup? {
        val selectQuery =
            "SELECT * FROM $TABLE_BACKUPS WHERE $KEY_BACKUP_ID = '${encrypt(id.toString())}'"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return getBackupFromCursor(cursor)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override val allBackups: List<Backup>?
        get() {
            val selectQuery = "SELECT * FROM $TABLE_BACKUPS"
            try {
                db.rawQuery(selectQuery, null).use { cursor ->

                    val list: MutableList<Backup> = ArrayList()
                    cursor?.let {
                        while (cursor.moveToNext()) {
                            getBackupFromCursor(cursor)?.let { it1 -> list.add(it1) }
                        }
                    }
                    return list
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception opening or managing DB cursor")
            }
            return null
        }

    private fun getBackupFromCursor(cursor: Cursor): Backup? {
        try {
            return Backup(backupId = (decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                KEY_BACKUP_ID))) ?: return null)
                .toLong(),
                backupType = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BACKUP_TYPE)),
                targetNode = (decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_TARGET_NODE))) ?: return null)
                    .toLong(),
                localFolder = decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_LOCAL_FOLDER))) ?: return null,
                backupName = decrypt(cursor.getString(cursor.getColumnIndexOrThrow(KEY_BACKUP_NAME)))!!,
                state = BackupState.fromValue(cursor.getInt(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_STATE))),
                subState = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BACKUP_SUB_STATE)),
                extraData = decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_EXTRA_DATA))) ?: return null,
                startTimestamp = (decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_START_TIME))) ?: return null)
                    .toLong(),
                lastFinishTimestamp = (decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_LAST_TIME))) ?: return null)
                    .toLong(),
                targetFolderPath = decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_TARGET_NODE_PATH))) ?: return null,
                isExcludeSubFolders = java.lang.Boolean.parseBoolean(decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_EX)))),
                isDeleteEmptySubFolders = java.lang.Boolean.parseBoolean(decrypt(cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        KEY_BACKUP_DEL)))),
                outdated = java.lang.Boolean.parseBoolean(decrypt(cursor.getString(cursor.getColumnIndexOrThrow(
                    KEY_BACKUP_OUTDATED))))
            )
        } catch (exception: IllegalArgumentException) {
            Timber.w(exception, "Exception getting Backup")
        }
        return null
    }

    override fun deleteBackupById(id: Long) {
        db.execSQL(deleteSQL(id))
    }

    override fun updateBackup(backup: Backup) {
        db.execSQL(updateSQL(backup))
    }

    override fun clearBackups() {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BACKUPS")
        onCreate(db)
    }

    override val isCompletedTransfersEmpty: Boolean
        get() = completedTransfers.isEmpty()

    override suspend fun getOfflineInformation(handle: Long): OfflineInformation? {
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_HANDLE = '${encrypt(handle.toString())}'"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(0).toInt()
                    val nodeHandle = decrypt(cursor.getString(1))
                    val path = decrypt(cursor.getString(2))
                    val name = decrypt(cursor.getString(3))
                    val parent = cursor.getInt(4)
                    val type = decrypt(cursor.getString(5))
                    val incoming = cursor.getInt(6)
                    val handleIncoming = decrypt(cursor.getString(7))
                    return OfflineInformation(
                        id = id,
                        handle = nodeHandle.toString(),
                        path = path.toString(),
                        name = name.toString(),
                        parentId = parent,
                        type = type,
                        origin = incoming,
                        handleIncoming = handleIncoming.toString(),
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception opening or managing DB cursor")
        }
        return null
    }

    override suspend fun getOfflineInformationList(
        nodePath: String,
        searchQuery: String?,
    ): List<OfflineInformation> {
        return if (searchQuery != null && searchQuery.isNotEmpty()) {
            searchOfflineInformationByQuery(nodePath, searchQuery)
        } else {
            searchOfflineInformationByPath(nodePath)
        }
    }

    /**
     * Search [OfflineInformation] by path
     *
     * @param nodePath
     * @return list of [OfflineInformation]
     */
    private fun searchOfflineInformationByPath(nodePath: String): List<OfflineInformation> {
        val offlineInformationList = mutableListOf<OfflineInformation>()
        //Get the foreign key of the node
        val selectQuery =
            "SELECT * FROM $TABLE_OFFLINE WHERE $KEY_OFF_PATH = '${encrypt(nodePath)}'"
        try {
            db.rawQuery(selectQuery, null)?.use { cursor ->
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
                        offlineInformationList.add(
                            OfflineInformation(
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
        return offlineInformationList
    }

    /**
     * Search [OfflineInformation] by query
     *
     * @param path
     * @param searchQuery
     * @return list of [OfflineInformation]
     */
    private fun searchOfflineInformationByQuery(
        path: String,
        searchQuery: String,
    ): List<OfflineInformation> {
        val offlineInformationList = mutableListOf<OfflineInformation>()
        val nodes = findByPath(path)
        for (node in nodes) {
            if (node.isFolder) {
                offlineInformationList.addAll(
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
                offlineInformationList.add(
                    OfflineInformation(
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
        return offlineInformationList
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

    /**
     * @return A update SQL with a given backup object.
     */
    private fun updateSQL(backup: Backup) =
        "UPDATE $TABLE_BACKUPS SET " +
                "$KEY_BACKUP_NAME = '${encrypt(backup.backupName)}', " +
                "$KEY_BACKUP_TYPE = ${backup.backupType}, " +
                "$KEY_BACKUP_LOCAL_FOLDER = '${encrypt(backup.localFolder)}', " +
                "$KEY_BACKUP_TARGET_NODE_PATH = '${encrypt(backup.targetFolderPath)}', " +
                "$KEY_BACKUP_TARGET_NODE = '${encrypt(backup.targetNode.toString())}', " +
                "$KEY_BACKUP_EX = '${encrypt(backup.isExcludeSubFolders.toString())}', " +
                "$KEY_BACKUP_DEL = '${encrypt(backup.isDeleteEmptySubFolders.toString())}', " +
                "$KEY_BACKUP_START_TIME = '${encrypt(backup.startTimestamp.toString())}', " +
                "$KEY_BACKUP_LAST_TIME = '${encrypt(backup.lastFinishTimestamp.toString())}', " +
                "$KEY_BACKUP_STATE = ${backup.state.value}, " +
                "$KEY_BACKUP_SUB_STATE = ${backup.subState}, " +
                "$KEY_BACKUP_EXTRA_DATA = '${encrypt(backup.extraData)}', " +
                "$KEY_BACKUP_OUTDATED = '${encrypt(backup.outdated.toString())}'" +
                "WHERE $KEY_BACKUP_ID = '${encrypt(backup.backupId.toString())}'"

    /**
     * @param id Backup id which is to be deleted.
     * @return A delete backup SQL.
     */
    private fun deleteSQL(id: Long) =
        "DELETE FROM $TABLE_BACKUPS WHERE $KEY_BACKUP_ID = '${encrypt(id.toString())}'"

    companion object {
        private const val DATABASE_VERSION = 67
        private const val DATABASE_NAME = "megapreferences"
        private const val TABLE_PREFERENCES = "preferences"
        private const val TABLE_CREDENTIALS = "credentials"
        private const val TABLE_ATTRIBUTES = "attributes"
        private const val TABLE_OFFLINE = "offline"
        private const val TABLE_CONTACTS = "contacts"
        private const val TABLE_CHAT_ITEMS = "chat"
        private const val TABLE_NON_CONTACTS = "noncontacts"
        private const val TABLE_CHAT_SETTINGS = "chatsettings"
        private const val TABLE_COMPLETED_TRANSFERS = "completedtransfers"
        private const val TABLE_EPHEMERAL = "ephemeral"
        private const val TABLE_PENDING_MSG = "pendingmsg"
        private const val TABLE_MSG_NODES = "msgnodes"
        private const val TABLE_NODE_ATTACHMENTS = "nodeattachments"
        private const val TABLE_PENDING_MSG_SINGLE = "pendingmsgsingle"
        private const val TABLE_SYNC_RECORDS = "syncrecords"
        private const val TABLE_MEGA_CONTACTS = "megacontacts"
        private const val TABLE_SD_TRANSFERS = "sdtransfers"
        const val TABLE_BACKUPS = "backups"
        private const val KEY_ID = "id"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_SESSION = "session"
        private const val KEY_FIRST_NAME = "firstname"
        private const val KEY_LAST_NAME = "lastname"
        private const val KEY_MY_HANDLE = "myhandle"
        private const val KEY_FIRST_LOGIN = "firstlogin"
        private const val KEY_CAM_SYNC_ENABLED = "camsyncenabled"
        private const val KEY_SEC_FOLDER_ENABLED = "secondarymediafolderenabled"
        private const val KEY_SEC_FOLDER_HANDLE = "secondarymediafolderhandle"
        private const val KEY_SEC_FOLDER_LOCAL_PATH = "secondarymediafolderlocalpath"
        private const val KEY_CAM_SYNC_HANDLE = "camsynchandle"
        private const val KEY_CAM_SYNC_WIFI = "wifi"
        private const val KEY_CAM_SYNC_LOCAL_PATH = "camsynclocalpath"
        private const val KEY_CAM_SYNC_FILE_UPLOAD = "fileUpload"
        private const val KEY_CAM_SYNC_TIMESTAMP = "camSyncTimeStamp"
        private const val KEY_CAM_VIDEO_SYNC_TIMESTAMP = "camVideoSyncTimeStamp"
        private const val KEY_UPLOAD_VIDEO_QUALITY = "uploadVideoQuality"
        private const val KEY_CONVERSION_ON_CHARGING = "conversionOnCharging"
        private const val KEY_REMOVE_GPS = "removeGPS"
        private const val KEY_CHARGING_ON_SIZE = "chargingOnSize"
        private const val KEY_SHOULD_CLEAR_CAMSYNC_RECORDS = "shouldclearcamsyncrecords"
        private const val KEY_KEEP_FILE_NAMES = "keepFileNames"
        private const val KEY_SHOW_INVITE_BANNER = "showinvitebanner"
        private const val KEY_ASK_FOR_DISPLAY_OVER = "askfordisplayover"
        private const val KEY_PASSCODE_LOCK_ENABLED = "pinlockenabled"
        private const val KEY_PASSCODE_LOCK_TYPE = "pinlocktype"
        private const val KEY_PASSCODE_LOCK_CODE = "pinlockcode"
        private const val KEY_PASSCODE_LOCK_REQUIRE_TIME = "passcodelockrequiretime"
        private const val KEY_FINGERPRINT_LOCK = "fingerprintlock"
        private const val KEY_STORAGE_ASK_ALWAYS = "storageaskalways"
        private const val KEY_STORAGE_DOWNLOAD_LOCATION = "storagedownloadlocation"
        private const val KEY_LAST_UPLOAD_FOLDER = "lastuploadfolder"
        private const val KEY_LAST_CLOUD_FOLDER_HANDLE = "lastcloudfolder"
        private const val KEY_ATTR_ONLINE = "online"
        private const val KEY_ATTR_INTENTS = "intents"
        private const val KEY_ATTR_ASK_SIZE_DOWNLOAD = "asksizedownload"
        private const val KEY_ATTR_ASK_NOAPP_DOWNLOAD = "asknoappdownload"
        private const val KEY_OFF_HANDLE = "handle"
        private const val KEY_OFF_PATH = "path"
        private const val KEY_OFF_NAME = "name"
        private const val KEY_OFF_PARENT = "parentId"
        private const val KEY_OFF_TYPE = "type"
        private const val KEY_OFF_INCOMING = "incoming"
        private const val KEY_OFF_HANDLE_INCOMING = "incomingHandle"
        private const val KEY_SEC_SYNC_TIMESTAMP = "secondarySyncTimeStamp"
        private const val KEY_SEC_VIDEO_SYNC_TIMESTAMP = "secondaryVideoSyncTimeStamp"
        private const val KEY_STORAGE_ADVANCED_DEVICES = "storageadvanceddevices"
        private const val KEY_ASK_SET_DOWNLOAD_LOCATION = "askSetDefaultDownloadLocation"
        private const val KEY_PREFERRED_VIEW_LIST = "preferredviewlist"
        private const val KEY_PREFERRED_VIEW_LIST_CAMERA = "preferredviewlistcamera"
        private const val KEY_URI_EXTERNAL_SD_CARD = "uriexternalsdcard"
        private const val KEY_URI_MEDIA_EXTERNAL_SD_CARD = "urimediaexternalsdcard"
        private const val KEY_SD_CARD_URI = "sdcarduri"
        private const val KEY_CAMERA_FOLDER_EXTERNAL_SD_CARD = "camerafolderexternalsdcard"
        private const val KEY_MEDIA_FOLDER_EXTERNAL_SD_CARD = "mediafolderexternalsdcard"
        private const val KEY_CONTACT_HANDLE = "handle"
        private const val KEY_CONTACT_MAIL = "mail"
        private const val KEY_CONTACT_NAME = "name"
        private const val KEY_CONTACT_LAST_NAME = "lastname"
        private const val KEY_CONTACT_NICKNAME = "nickname"
        private const val KEY_PREFERRED_SORT_CLOUD = "preferredsortcloud"
        private const val KEY_PREFERRED_SORT_CAMERA_UPLOAD = "preferredsortcameraupload"
        private const val KEY_PREFERRED_SORT_OTHERS = "preferredsortothers"
        private const val KEY_FILE_LOGGER_SDK = "filelogger"
        private const val KEY_FILE_LOGGER_KARERE = "fileloggerkarere"
        private const val KEY_USE_HTTPS_ONLY = "usehttpsonly"
        private const val KEY_SHOW_COPYRIGHT = "showcopyright"
        private const val KEY_SHOW_NOTIF_OFF = "shownotifoff"
        private const val KEY_ACCOUNT_DETAILS_TIMESTAMP = "accountdetailstimestamp"
        @Deprecated("Unused database properties")
        private const val KEY_PAYMENT_METHODS_TIMESTAMP = "paymentmethodsstimestamp"
        @Deprecated("Unused database properties")
        private const val KEY_PRICING_TIMESTAMP = "pricingtimestamp"
        private const val KEY_EXTENDED_ACCOUNT_DETAILS_TIMESTAMP = "extendedaccountdetailstimestamp"
        private const val KEY_CHAT_HANDLE = "chathandle"
        private const val KEY_CHAT_ITEM_NOTIFICATIONS = "chatitemnotifications"
        private const val KEY_CHAT_ITEM_RINGTONE = "chatitemringtone"
        private const val KEY_CHAT_ITEM_SOUND_NOTIFICATIONS = "chatitemnotificationsound"
        private const val KEY_CHAT_ITEM_WRITTEN_TEXT = "chatitemwrittentext"
        private const val KEY_CHAT_ITEM_EDITED_MSG_ID = "chatitemeditedmsgid"
        private const val KEY_NONCONTACT_HANDLE = "noncontacthandle"
        private const val KEY_NONCONTACT_FULLNAME = "noncontactfullname"
        private const val KEY_NONCONTACT_FIRSTNAME = "noncontactfirstname"
        private const val KEY_NONCONTACT_LASTNAME = "noncontactlastname"
        private const val KEY_NONCONTACT_EMAIL = "noncontactemail"
        private const val KEY_CHAT_NOTIFICATIONS_ENABLED = "chatnotifications"
        private const val KEY_CHAT_SOUND_NOTIFICATIONS = "chatnotificationsound"
        private const val KEY_CHAT_VIBRATION_ENABLED = "chatvibrationenabled"
        private const val KEY_CHAT_VIDEO_QUALITY = "chatvideoQuality"
        private const val KEY_INVALIDATE_SDK_CACHE = "invalidatesdkcache"
        private const val KEY_TRANSFER_FILENAME = "transferfilename"
        private const val KEY_TRANSFER_TYPE = "transfertype"
        private const val KEY_TRANSFER_STATE = "transferstate"
        private const val KEY_TRANSFER_SIZE = "transfersize"
        private const val KEY_TRANSFER_HANDLE = "transferhandle"
        private const val KEY_TRANSFER_PATH = "transferpath"
        private const val KEY_TRANSFER_OFFLINE = "transferoffline"
        private const val KEY_TRANSFER_TIMESTAMP = "transfertimestamp"
        private const val KEY_TRANSFER_ERROR = "transfererror"
        private const val KEY_TRANSFER_ORIGINAL_PATH = "transferoriginalpath"
        private const val KEY_TRANSFER_PARENT_HANDLE = "transferparenthandle"
        private const val KEY_FIRST_LOGIN_CHAT = "firstloginchat"
        private const val KEY_AUTO_PLAY = "autoplay"
        private const val KEY_ID_CHAT = "idchat"
        private const val KEY_MSG_TIMESTAMP = "timestamp"
        private const val KEY_ID_TEMP_KARERE = "idtempkarere"
        private const val KEY_STATE = "state"
        private const val KEY_ID_PENDING_MSG = "idpendingmsg"
        private const val KEY_ID_NODE = "idnode"
        private const val KEY_FILE_PATH = "filepath"
        private const val KEY_FILE_NAME = "filename"
        private const val KEY_FILE_FINGERPRINT = "filefingerprint"
        private const val KEY_NODE_HANDLE = "nodehandle"

        //columns for table sync records
        private const val KEY_SYNC_FILEPATH_ORI = "sync_filepath_origin"
        private const val KEY_SYNC_FILEPATH_NEW = "sync_filepath_new"
        private const val KEY_SYNC_FP_ORI = "sync_fingerprint_origin"
        private const val KEY_SYNC_FP_NEW = "sync_fingerprint_new"
        private const val KEY_SYNC_TIMESTAMP = "sync_timestamp"
        private const val KEY_SYNC_STATE = "sync_state"
        private const val KEY_SYNC_FILENAME = "sync_filename"
        private const val KEY_SYNC_HANDLE = "sync_handle"
        private const val KEY_SYNC_COPYONLY = "sync_copyonly"
        private const val KEY_SYNC_SECONDARY = "sync_secondary"
        private const val KEY_SYNC_TYPE = "sync_type"
        private const val KEY_SYNC_LONGITUDE = "sync_longitude"
        private const val KEY_SYNC_LATITUDE = "sync_latitude"
        private const val CREATE_SYNC_RECORDS_TABLE =
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
        private const val KEY_LAST_PUBLIC_HANDLE = "lastpublichandle"
        private const val KEY_LAST_PUBLIC_HANDLE_TIMESTAMP = "lastpublichandletimestamp"
        private const val KEY_LAST_PUBLIC_HANDLE_TYPE = "lastpublichandletype"
        private const val KEY_STORAGE_STATE = "storagestate"
        private const val KEY_MY_CHAT_FILES_FOLDER_HANDLE = "mychatfilesfolderhandle"
        private const val KEY_TRANSFER_QUEUE_STATUS = "transferqueuestatus"
        private const val KEY_PENDING_MSG_ID_CHAT = "idchat"
        private const val KEY_PENDING_MSG_TIMESTAMP = "timestamp"
        private const val KEY_PENDING_MSG_TEMP_KARERE = "idtempkarere"
        private const val KEY_PENDING_MSG_FILE_PATH = "filePath"
        private const val KEY_PENDING_MSG_NAME = "filename"
        private const val KEY_PENDING_MSG_NODE_HANDLE = "nodehandle"
        private const val KEY_PENDING_MSG_FINGERPRINT = "filefingerprint"
        private const val KEY_PENDING_MSG_TRANSFER_TAG = "transfertag"
        private const val KEY_PENDING_MSG_STATE = "state"
        private const val KEY_MEGA_CONTACTS_ID = "userid"
        private const val KEY_MEGA_CONTACTS_HANDLE = "handle"
        private const val KEY_MEGA_CONTACTS_LOCAL_NAME = "localname"
        private const val KEY_MEGA_CONTACTS_EMAIL = "email"
        private const val KEY_MEGA_CONTACTS_PHONE_NUMBER = "phonenumber"
        private const val CREATE_MEGA_CONTACTS_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_MEGA_CONTACTS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +
                    "$KEY_MEGA_CONTACTS_ID TEXT," +
                    "$KEY_MEGA_CONTACTS_HANDLE TEXT," +
                    "$KEY_MEGA_CONTACTS_LOCAL_NAME TEXT," +
                    "$KEY_MEGA_CONTACTS_EMAIL TEXT," +
                    "$KEY_MEGA_CONTACTS_PHONE_NUMBER TEXT)"
        private const val KEY_SD_TRANSFERS_TAG = "sdtransfertag"
        private const val KEY_SD_TRANSFERS_NAME = "sdtransfername"
        private const val KEY_SD_TRANSFERS_SIZE = "sdtransfersize"
        private const val KEY_SD_TRANSFERS_HANDLE = "sdtransferhandle"
        private const val KEY_SD_TRANSFERS_APP_DATA = "sdtransferappdata"
        private const val KEY_SD_TRANSFERS_PATH = "sdtransferpath"
        private const val CREATE_SD_TRANSFERS_TABLE =
            "CREATE TABLE IF NOT EXISTS $TABLE_SD_TRANSFERS(" +
                    "$KEY_ID INTEGER PRIMARY KEY, " +                     // 0
                    "$KEY_SD_TRANSFERS_TAG INTEGER, " +                   // 1
                    "$KEY_SD_TRANSFERS_NAME TEXT, " +                     // 2
                    "$KEY_SD_TRANSFERS_SIZE TEXT, " +                     // 3
                    "$KEY_SD_TRANSFERS_HANDLE TEXT, " +                   // 4
                    "$KEY_SD_TRANSFERS_PATH TEXT, " +                     // 5
                    "$KEY_SD_TRANSFERS_APP_DATA TEXT)"                    // 6
        private const val KEY_BACKUP_ID = "backup_id"
        private const val KEY_BACKUP_TYPE = "backup_type"
        private const val KEY_BACKUP_TARGET_NODE = "target_node"
        private const val KEY_BACKUP_LOCAL_FOLDER = "local_folder"
        private const val KEY_BACKUP_NAME = "backup_name"
        private const val KEY_BACKUP_STATE = "state"
        private const val KEY_BACKUP_SUB_STATE = "sub_state"
        private const val KEY_BACKUP_EXTRA_DATA = "extra_data"
        private const val KEY_BACKUP_START_TIME = "start_timestamp"
        private const val KEY_BACKUP_LAST_TIME = "last_sync_timestamp"
        private const val KEY_BACKUP_TARGET_NODE_PATH = "target_folder_path"
        private const val KEY_BACKUP_EX = "exclude_subolders"
        private const val KEY_BACKUP_DEL = "delete_empty_subolders"
        private const val KEY_BACKUP_OUTDATED = "outdated"
        private const val CREATE_BACKUP_TABLE = "CREATE TABLE IF NOT EXISTS $TABLE_BACKUPS(" +
                "$KEY_ID INTEGER PRIMARY KEY, " +                          //0
                "$KEY_BACKUP_ID TEXT, " +                                  //1
                "$KEY_BACKUP_TYPE INTEGER," +                              //2
                "$KEY_BACKUP_TARGET_NODE TEXT," +                          //3
                "$KEY_BACKUP_LOCAL_FOLDER TEXT," +                         //4
                "$KEY_BACKUP_NAME TEXT," +                                 //5
                "$KEY_BACKUP_STATE INTEGER," +                             //6
                "$KEY_BACKUP_SUB_STATE INTEGER," +                         //7
                "$KEY_BACKUP_EXTRA_DATA TEXT," +                           //8
                "$KEY_BACKUP_START_TIME TEXT," +                           //9
                "$KEY_BACKUP_LAST_TIME TEXT," +                            //10
                "$KEY_BACKUP_TARGET_NODE_PATH TEXT," +                     //11
                "$KEY_BACKUP_EX BOOLEAN," +                                //12
                "$KEY_BACKUP_DEL BOOLEAN," +                               //13
                "$KEY_BACKUP_OUTDATED BOOLEAN)"                            //14
        private const val OLD_VIDEO_QUALITY_ORIGINAL = 0
        private const val SYNC_RECORD_TYPE_VIDEO = 2
        private const val SYNC_RECORD_TYPE_ANY = -1
        private var instance: DatabaseHandler? = null

        @JvmStatic
        @Synchronized
        fun getDbHandler(
            context: Context,
            storageStateMapper: StorageStateMapper,
            storageStateIntMapper: StorageStateIntMapper,
        ): LegacyDatabaseHandler {
            Timber.d("getDbHandler")

            if (instance == null) {
                Timber.d("INSTANCE IS NULL")
                val legacyLoggingSettings = fromApplication(
                    context,
                    LegacyLoggingEntryPoint::class.java
                ).legacyLoggingSettings

                instance = SqliteDatabaseHandler(
                    context = context,
                    legacyLoggingSettings = legacyLoggingSettings,
                    storageStateMapper = storageStateMapper,
                    storageStateIntMapper = storageStateIntMapper
                )
            }
            return instance as LegacyDatabaseHandler
        }

        private fun encrypt(original: String?): String? =
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

        private fun decrypt(encodedString: String?): String? =
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

    init {
        db = this.writableDatabase
    }
}
