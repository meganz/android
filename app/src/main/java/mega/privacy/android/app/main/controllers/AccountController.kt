package mega.privacy.android.app.main.controllers

import android.Manifest
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.app.utils.StorageUtils.thereIsNotEnoughFreeSpace
import mega.privacy.android.app.utils.CacheFolderManager.removeOldTempFolders
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.stopAudioPlayer
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceViewModel.Companion.clearSettings
import mega.privacy.android.app.psa.PsaManager.stopChecking
import mega.privacy.android.app.sync.removeBackupsBeforeLogout
import nz.mega.sdk.MegaApiAndroid
import android.graphics.Bitmap
import androidx.print.PrintHelper
import android.app.Activity
import android.app.NotificationManager
import android.content.*
import androidx.core.content.ContextCompat
import mega.privacy.android.app.utils.contacts.MegaContactGetter
import mega.privacy.android.app.middlelayer.push.PushMessageHandler
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.fragments.offline.OfflineFragment
import mega.privacy.android.app.constants.SettingsConstants
import nz.mega.sdk.MegaApiJava
import com.jeremyliao.liveeventbus.LiveEventBus
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mega.privacy.android.app.*
import mega.privacy.android.app.data.preferences.ChatPreferencesDataStore
import mega.privacy.android.app.domain.entity.SyncRecordType
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.*
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ChatUtil.removeEmojisSharedPreferences
import mega.privacy.android.app.utils.FileUtil.*
import mega.privacy.android.app.utils.JobUtil.*
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.*
import mega.privacy.android.app.utils.ZoomUtil.resetZoomLevel
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaError
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalStateException

class AccountController(private val context: Context) {

    fun existsAvatar(): Boolean {
        val avatar = buildAvatarFile(
            context, MegaApplication.getInstance().megaApi.myEmail + JPG_EXTENSION
        )

        if (avatar?.exists() == true) {
            Timber.d("Avatar exists in: " + avatar.absolutePath)
            return true
        }

        return false
    }

    fun printRK() {
        val rKBitmap = createRkBitmap()

        if (rKBitmap != null) {
            PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
                printBitmap("rKPrint", rKBitmap) {
                    if (context is TestPasswordActivity) {
                        context.passwordReminderSucceeded()
                    }
                }
            }
        }
    }

    /**
     * Export recovery key file to a selected location on file system.
     *
     * @param path The selected location.
     */
    fun exportMK(path: String?) {
        Timber.d("exportMK")

        if (isOffline(context)) {
            return
        }

        val megaApi = MegaApplication.getInstance().megaApi
        val key = megaApi.exportMasterKey()

        if (context is ManagerActivity) {
            megaApi.masterKeyExported(context)
        } else if (context is TestPasswordActivity) {
            context.incrementRequests()
            megaApi.masterKeyExported(context)
        }

        if (!hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (context is ManagerActivity) {
                requestPermission(
                    context,
                    Constants.REQUEST_WRITE_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } else if (context is TestPasswordActivity) {
                requestPermission(
                    context,
                    Constants.REQUEST_WRITE_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }

            return
        }

        if (thereIsNotEnoughFreeSpace(path!!)) {
            showSnackbar(context, getString(R.string.error_not_enough_free_space))
            return
        }

        if (saveTextOnFile(context, key, path)) {
            showSnackbar(context, getString(R.string.save_MK_confirmation))

            if (context is TestPasswordActivity) {
                context.passwordReminderSucceeded()
            }
        }
    }

    /**
     * Rename the old MK or RK file to the new RK file name.
     * @param oldFile Old MK or RK file to be renamed
     */
    fun renameRK(oldFile: File) {
        Timber.d("renameRK")
        val newRKFile = File(oldFile.parentFile, getRecoveryKeyFileName())
        oldFile.renameTo(newRKFile)
    }

    fun copyMK(logout: Boolean, sharingScope: CoroutineScope) {
        Timber.d("copyMK")
        val megaApi = MegaApplication.getInstance().megaApi
        val key = megaApi.exportMasterKey()

        if (context is ManagerActivity) {
            if (key != null) {
                megaApi.masterKeyExported(context)
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", key)
                clipboard.setPrimaryClip(clip)

                if (logout) {
                    showConfirmDialogRecoveryKeySaved(sharingScope)
                } else {
                    showAlert(context, getString(R.string.copy_MK_confirmation), null)
                }
            } else {
                showAlert(context, getString(R.string.general_text_error), null)
            }
        } else if (context is TestPasswordActivity) {
            if (key != null) {
                context.incrementRequests()
                megaApi.masterKeyExported(context)
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", key)
                clipboard.setPrimaryClip(clip)

                if (logout) {
                    showConfirmDialogRecoveryKeySaved(sharingScope)
                } else {
                    context.showSnackbar(getString(R.string.copy_MK_confirmation))
                    context.passwordReminderSucceeded()
                }
            } else {
                context.showSnackbar(getString(R.string.general_text_error))
            }
        }
    }

    fun copyRkToClipboard(sharingScope: CoroutineScope) {
        Timber.d("copyRkToClipboard")
        when (context) {
            is ManagerActivity -> {
                copyMK(false, sharingScope)
            }
            is TestPasswordActivity -> {
                copyMK(context.isLogout, sharingScope)
            }
            is TwoFactorAuthenticationActivity -> {
                val intent = Intent(context, ManagerActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.action = Constants.ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD
                intent.putExtra("logout", false)
                context.startActivity(intent)
                context.finish()
            }
        }
    }

    private fun createRkBitmap(): Bitmap? {
        Timber.d("createRkBitmap")
        val rKBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val key = MegaApplication.getInstance().megaApi.exportMasterKey()

        if (key != null) {
            val canvas = Canvas(rKBitmap!!)
            val paint = Paint()
            paint.textSize = 40f
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL
            val height = paint.measureText("yY")
            val width = paint.measureText(key)
            val x = (rKBitmap.width - width) / 2
            canvas.drawText(key, x, height + 15f, paint)
            return rKBitmap
        }

        showAlert(context as ManagerActivity, getString(R.string.general_text_error), null)
        return null
    }

    fun showConfirmDialogRecoveryKeySaved(sharingScope: CoroutineScope) {
        AlertDialog.Builder(context).apply {
            setMessage(getString(R.string.copy_MK_confirmation))
            setPositiveButton(getString(R.string.action_logout)) { _: DialogInterface?, _: Int ->
                if (this@AccountController.context is TestPasswordActivity) {
                    this@AccountController.context.passwordReminderSucceeded()
                } else {
                    logout(context, MegaApplication.getInstance().megaApi, sharingScope)
                }
            }
            setOnDismissListener {
                if (this@AccountController.context is TestPasswordActivity) {
                    this@AccountController.context.passwordReminderSucceeded()
                }
            }
            show()
        }
    }

    companion object {
        @JvmStatic
        fun saveRkToFileSystem(activity: Activity) {
            val intent = Intent(activity, FileStorageActivity::class.java)
                .setAction(FileStorageActivity.Mode.PICK_FOLDER.action)
                .putExtra(FileStorageActivity.EXTRA_SAVE_RECOVERY_KEY, true)
            activity.startActivityForResult(intent, Constants.REQUEST_DOWNLOAD_FOLDER)
        }

        @JvmStatic
        fun localLogoutApp(context: Context, sharingScope: CoroutineScope) {
            val app = MegaApplication.getInstance()
            Timber.d("Logged out. Resetting account auth token for folder links.")
            app.megaApiFolder.accountAuth = null

            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
            } catch (e: Exception) {
                Timber.e("EXCEPTION removing all the notifications", e)
                e.printStackTrace()
            }

            removeFolder(context, context.filesDir)
            removeFolder(context, context.externalCacheDir)

            val downloadToSDCardCache = context.externalCacheDirs
            if (downloadToSDCardCache.size > 1) {
                removeFolder(context, downloadToSDCardCache[1])
            }

            removeFolder(context, context.cacheDir)
            removeOldTempFolders(context)

            try {
                var cancelTransfersIntent = Intent(context, DownloadService::class.java)
                cancelTransfersIntent.action = DownloadService.ACTION_CANCEL
                context.startService(cancelTransfersIntent)
                cancelTransfersIntent = Intent(context, UploadService::class.java)
                cancelTransfersIntent.action = UploadService.ACTION_CANCEL
                ContextCompat.startForegroundService(context, cancelTransfersIntent)
            } catch (e: IllegalStateException) {
                //If the application is in a state where the service can not be started (such as not in the foreground in a state when services are allowed) - included in API 26
                Timber.w("Cancelling services not allowed by the OS", e)
            }

            val dbH = DatabaseHandler.getDbHandler(context)
            dbH.clearCredentials()

            if (dbH.preferences != null) {
                dbH.clearPreferences()
                dbH.setFirstTime(false)
                fireStopCameraUploadJob(context);
                stopCameraUploadSyncHeartbeatWorkers(context);
            }

            dbH.clearOffline()
            dbH.clearContacts()
            dbH.clearNonContacts()
            dbH.clearChatItems()
            dbH.clearCompletedTransfers()
            dbH.clearPendingMessage()
            dbH.clearAttributes()
            dbH.deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            dbH.clearChatSettings()
            dbH.clearBackups()

            //clear mega contacts and reset last sync time.
            dbH.clearMegaContacts()
            CoroutineScope(Dispatchers.IO).launch {
                MegaContactGetter(context).clearLastSyncTimeStamp()
            }
            // clean time stamps preference settings after logout
            context.getSharedPreferences(
                MegaContactGetter.LAST_SYNC_TIMESTAMP_FILE,
                Context.MODE_PRIVATE
            ).edit()
                .clear()
                .putLong(MegaContactGetter.LAST_SYNC_TIMESTAMP_KEY, 0)
                .apply()

            //clear push token
            context.getSharedPreferences(PushMessageHandler.PUSH_TOKEN, Context.MODE_PRIVATE).edit()
                .clear().apply()

            //clear user interface preferences
            context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
                .edit().clear().apply()

            //clear chat preferences
            sharingScope.launch {
                ChatPreferencesDataStore(context, Dispatchers.IO).clearPreferences()
            }

            // Clear text editor preference
            // Clear offline warning preference
            // Clear Key mobile data high resolution preference
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(TextEditorViewModel.SHOW_LINE_NUMBERS, false)
                .putBoolean(OfflineFragment.SHOW_OFFLINE_WARNING, true)
                .remove(SettingsConstants.KEY_MOBILE_DATA_HIGH_RESOLUTION)
                .apply()

            //reset zoom level
            resetZoomLevel()
            removeEmojisSharedPreferences()
            LastShowSMSDialogTimeChecker(context).reset()
            stopAudioPlayer(context)
            clearSettings(context)
            stopChecking()

            //Clear MyAccountInfo
            app.resetMyAccountInfo()
            app.storageState = MegaApiJava.STORAGE_STATE_UNKNOWN

            // Clear get banner success flag
            LiveEventBus.get(Constants.EVENT_LOGOUT_CLEARED).post(null)
        }

        fun removeFolder(context: Context?, folder: File?) {
            try {
                deleteFolderAndSubfolders(context, folder)
            } catch (e: IOException) {
                Timber.e("Exception deleting" + folder!!.name + "directory", e)
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun logout(context: Context, megaApi: MegaApiAndroid, sharingScope: CoroutineScope) {
            Timber.d("logout")
            MegaApplication.setLoggingOut(true)
            removeBackupsBeforeLogout()

            when (context) {
                is ManagerActivity -> megaApi.logout(context)
                is OpenLinkActivity -> megaApi.logout(context)
                is TestPasswordActivity -> megaApi.logout(context)
                else -> megaApi.logout(OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        logoutConfirmed(context, sharingScope)

                        context.startActivity(
                            Intent(
                                context,
                                if (context is MeetingActivity) LeftMeetingActivity::class.java
                                else LoginActivity::class.java
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )

                        (context as Activity).finish()
                    } else {
                        showSnackbar(
                            context,
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.general_error),
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    }
                }))
            }

            context.sendBroadcast(Intent().setAction(Constants.ACTION_LOG_OUT))
        }

        @JvmStatic
        fun logoutConfirmed(context: Context, sharingScope: CoroutineScope) {
            Timber.d("logoutConfirmed")
            localLogoutApp(context, sharingScope)
            val m = context.packageManager
            var s = context.packageName

            try {
                val p = m.getPackageInfo(s!!, 0)
                s = p.applicationInfo.dataDir
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.d("Error Package name not found $e")
            }

            val files = File(s).listFiles()

            if (files != null) {
                for (c in files) {
                    if (c.isFile) {
                        c.delete()
                    }
                }
            }
        }
    }
}