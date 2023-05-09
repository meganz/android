package mega.privacy.android.app.main.controllers

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.print.PrintHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.OpenLinkActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.fragments.offline.OfflineFragment
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.stopAudioPlayer
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceViewModel.Companion.clearSettings
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity
import mega.privacy.android.app.presentation.twofactorauthentication.TwoFactorAuthenticationActivity
import mega.privacy.android.app.psa.PsaManager.stopChecking
import mega.privacy.android.app.sync.removeBackupsBeforeLogout
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.CacheFolderManager.removeOldTempFolders
import mega.privacy.android.app.utils.ChatUtil.removeEmojisSharedPreferences
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil.deleteFolderAndSubfolders
import mega.privacy.android.app.utils.FileUtil.getRecoveryKeyFileName
import mega.privacy.android.app.utils.FileUtil.saveTextOnFile
import mega.privacy.android.app.utils.LastShowSMSDialogTimeChecker
import mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES
import mega.privacy.android.app.utils.StorageUtils.thereIsNotEnoughFreeSpace
import mega.privacy.android.app.utils.Util.isOffline
import mega.privacy.android.app.utils.Util.showAlert
import mega.privacy.android.app.utils.Util.showSnackbar
import mega.privacy.android.app.utils.contacts.MegaContactGetter
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.data.gateway.preferences.AccountPreferencesGateway
import mega.privacy.android.data.gateway.preferences.CallsPreferencesGateway
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.usecase.login.BroadcastLogoutUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadAndHeartbeatUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaError
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

@ActivityScoped
class AccountController @Inject constructor(
    @ActivityContext private val context: Context,
) {
    /**
     * Account controller entry point
     *
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface AccountControllerEntryPoint {
        /**
         * Chat preferences gateway
         *
         */
        fun chatPreferencesGateway(): ChatPreferencesGateway

        /**
         * Calls preferences gateway
         *
         */
        fun callsPreferencesGateway(): CallsPreferencesGateway

        /**
         * Account preferences gateway
         */
        fun accountPreferencesGateway(): AccountPreferencesGateway

        /**
         * Push repository
         *
         */
        fun pushRepository(): PushesRepository

        fun billingRepository(): BillingRepository

        fun broadcastLogout(): BroadcastLogoutUseCase

        fun photosRepository(): PhotosRepository

        fun albumRepository(): AlbumRepository

        fun stopCameraUploadUseCase(): StopCameraUploadUseCase

        fun stopCameraUploadAndHeartbeatUseCase(): StopCameraUploadAndHeartbeatUseCase
    }

    fun printRK(onAfterPrint: () -> Unit = {}) {
        val rKBitmap = createRkBitmap()

        if (rKBitmap != null) {
            PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
                printBitmap("rKPrint", rKBitmap) {
                    onAfterPrint()
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
            showSnackbar(context, context.getString(R.string.error_not_enough_free_space))
            return
        }

        if (saveTextOnFile(context, key, path)) {
            showSnackbar(context, context.getString(R.string.save_MK_confirmation))

            if (context is TestPasswordActivity) {
                context.onRecoveryKeyExported()
            }
        }
    }

    /**
     * Rename the old MK or RK file to the new RK file name.
     * @param oldFile Old MK or RK file to be renamed
     */
    fun renameRK(oldFile: File) {
        Timber.d("renameRK")
        val newRKFile = File(oldFile.parentFile, getRecoveryKeyFileName(context))
        oldFile.renameTo(newRKFile)
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

        showAlert(context, context.getString(R.string.general_text_error), null)
        return null
    }

    fun showConfirmDialogRecoveryKeySaved(sharingScope: CoroutineScope) {
        AlertDialog.Builder(context).apply {
            setMessage(context.getString(R.string.copy_MK_confirmation))
            setPositiveButton(context.getString(R.string.action_logout)) { _: DialogInterface?, _: Int ->
                logout(context, MegaApplication.getInstance().megaApi, sharingScope)
            }
            show()
        }
    }

    companion object {

        @Deprecated(
            message = "It has been deprecated in favour of LocalLogoutAppUseCase",
            replaceWith = ReplaceWith(
                expression = "mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase"
            )
        )
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
                Timber.w(e, "Cancelling services not allowed by the OS")
            }

            val dbH = getDbHandler()
            dbH.clearCredentials()

            if (dbH.preferences != null) {
                dbH.clearPreferences()
                dbH.setFirstTime(false)
            }

            dbH.clearOffline()
            dbH.clearContacts()
            dbH.clearNonContacts()
            dbH.clearChatItems()
            dbH.clearCompletedTransfers()
            dbH.clearAttributes()
            dbH.deleteAllSyncRecordsTypeAny()
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

            //clear user interface preferences
            context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
                .edit().clear().apply()

            //clear chat and calls preferences
            val entryPoint =
                EntryPointAccessors.fromApplication(
                    context,
                    AccountControllerEntryPoint::class.java
                )
            sharingScope.launch(Dispatchers.IO) {
                with(entryPoint) {
                    stopCameraUploadUseCase()
                    stopCameraUploadAndHeartbeatUseCase()
                    callsPreferencesGateway().clearPreferences()
                    chatPreferencesGateway().clearPreferences()
                    accountPreferencesGateway().clearPreferences()
                    pushRepository().clearPushToken()
                    billingRepository().clearCache()
                    photosRepository().clearCache()
                    albumRepository().clearCache()
                    broadcastLogout()
                }
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

            removeEmojisSharedPreferences()
            LastShowSMSDialogTimeChecker(context).reset()
            stopAudioPlayer(context)
            clearSettings(context)
            stopChecking()

            //Clear MyAccountInfo
            app.resetMyAccountInfo()
        }

        fun removeFolder(context: Context?, folder: File?) {
            try {
                deleteFolderAndSubfolders(context, folder)
            } catch (e: IOException) {
                Timber.e(e, "Exception deleting ${folder?.name} directory")
            }
        }

        @JvmStatic
        fun logout(context: Context, megaApi: MegaApiAndroid, sharingScope: CoroutineScope) {
            Timber.d("logout")
            MegaApplication.isLoggingOut = true
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
                            context.getString(R.string.general_error),
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
                val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    m.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    m.getPackageInfo(context.packageName, 0)
                }

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
