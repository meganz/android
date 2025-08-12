package mega.privacy.android.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.INITIAL_PREFERENCE
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.NAVIGATE_TO_INITIAL_PREFERENCE
import mega.privacy.android.app.presentation.settings.camerauploads.INTENT_EXTRA_KEY_SHOW_DISABLE_CU
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.presentation.settings.compose.SettingsHomeActivity
import mega.privacy.android.app.presentation.settings.model.cameraUploadsTargetPreference
import mega.privacy.android.app.presentation.transfers.notification.OpenTransfersSectionIntentMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera Uploads Notification Manager
 */
@Singleton
class CameraUploadsNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stringWrapper: StringWrapper,
    private val getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase,
    private val openTransfersSectionIntentMapper: OpenTransfersSectionIntentMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID =
            Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID
        private const val NOTIFICATION_CHANNEL_NAME =
            Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME
        private const val NOTIFICATION_ID =
            Constants.NOTIFICATION_CAMERA_UPLOADS
        private const val COMPRESSION_NOTIFICATION_ID =
            Constants.NOTIFICATION_VIDEO_COMPRESSION
        private const val PRIMARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID =
            Constants.NOTIFICATION_CAMERA_UPLOADS_PRIMARY_FOLDER_UNAVAILABLE
        private const val SECONDARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID =
            Constants.NOTIFICATION_CAMERA_UPLOADS_SECONDARY_FOLDER_UNAVAILABLE
        private const val COMPRESSION_ERROR_NOTIFICATION_ID =
            Constants.NOTIFICATION_COMPRESSION_ERROR
        private const val NOT_ENOUGH_STORAGE_NOTIFICATION_ID =
            Constants.NOTIFICATION_NOT_ENOUGH_STORAGE
        private const val OVER_STORAGE_QUOTA_NOTIFICATION_ID =
            Constants.NOTIFICATION_STORAGE_OVERQUOTA
        private const val NO_WIFI_CONNECTION_NOTIFICATION_ID =
            Constants.NOTIFICATION_NO_WIFI_CONNECTION
        private const val NO_NETWORK_CONNECTION_NOTIFICATION_ID =
            Constants.NOTIFICATION_NO_NETWORK_CONNECTION
        private const val ACTION_SHOW_SETTINGS = Constants.ACTION_SHOW_SETTINGS
        private const val ACTION_OVER_QUOTA_STORAGE = Constants.ACTION_OVERQUOTA_STORAGE
    }

    /**
     * Notification manager used to display notifications
     */
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     *  Show Notification
     */

    suspend fun showNotification(cameraUploadsStatusInfo: CameraUploadsStatusInfo) {
        when (cameraUploadsStatusInfo) {
            is CameraUploadsStatusInfo.Unknown -> {}
            is CameraUploadsStatusInfo.Started -> cancelAllNotifications()
            is CameraUploadsStatusInfo.Finished -> {
                cancelNotification()
                cancelCompressionNotification()
            }

            CameraUploadsStatusInfo.CheckFilesForUpload -> showCheckUploadsNotification()
            is CameraUploadsStatusInfo.FolderUnavailable -> showFolderUnavailableNotification(
                cameraUploadsStatusInfo.cameraUploadsFolderType
            )

            CameraUploadsStatusInfo.NotEnoughStorage -> showNotEnoughStorageNotification()
            is CameraUploadsStatusInfo.UploadProgress -> showUploadProgressNotification(
                totalUploaded = cameraUploadsStatusInfo.totalUploaded,
                totalToUpload = cameraUploadsStatusInfo.totalToUpload,
                totalUploadedBytes = cameraUploadsStatusInfo.totalUploadedBytes,
                totalUploadBytes = cameraUploadsStatusInfo.totalUploadBytes,
                progress = cameraUploadsStatusInfo.progress.intValue,
                areUploadsPaused = cameraUploadsStatusInfo.areUploadsPaused,
            )

            CameraUploadsStatusInfo.StorageOverQuota -> showStorageOverQuotaNotification()

            is CameraUploadsStatusInfo.VideoCompressionSuccess -> {
                cancelCompressionNotification()
            }

            CameraUploadsStatusInfo.VideoCompressionError -> {
                cancelCompressionNotification()
                showVideoCompressionErrorNotification()
            }

            CameraUploadsStatusInfo.VideoCompressionOutOfSpace -> showVideoCompressionOutOfSpaceNotification()
            is CameraUploadsStatusInfo.VideoCompressionProgress -> showVideoCompressionProgressNotification(
                progress = cameraUploadsStatusInfo.progress.intValue,
                currentFileIndex = cameraUploadsStatusInfo.currentFileIndex,
                totalCount = cameraUploadsStatusInfo.totalCount,
            )

            CameraUploadsStatusInfo.NoWifiConnection -> showNoWifiConnectionNotification()
            CameraUploadsStatusInfo.NoNetworkConnection -> showNoNetworkConnectionNotification()
        }
    }

    private fun createNotification(
        title: String,
        content: String,
        subText: String? = null,
        intent: PendingIntent? = null,
        actionText: String? = null,
        actionIntent: PendingIntent? = null,
        isOngoing: Boolean = false,
        progress: Int? = null,
        isAutoCancel: Boolean = true,
        action: NotificationCompat.Action? = null,
    ): Notification {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.setShowBadge(false)
        channel.setSound(null, null)
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(
            context,
            NOTIFICATION_CHANNEL_ID
        ).apply {
            setSmallIcon(IconPackR.drawable.ic_stat_camera_uploads_running)
            setOngoing(isOngoing)
            setContentTitle(title)
            setStyle(NotificationCompat.BigTextStyle().bigText(content))
            setContentText(content)
            setOnlyAlertOnce(true)
            setAutoCancel(isAutoCancel)
            intent?.let { setContentIntent(intent) }
            progress?.let { setProgress(100, progress, false) }
            subText?.let { setSubText(subText) }
            action?.let { addAction(action) }
            actionIntent?.let {
                addAction(
                    NotificationCompat.Action.Builder(
                        IconPackR.drawable.ic_stat_camera_uploads_running,
                        actionText,
                        actionIntent
                    ).build()
                )
            }
            foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
        }
        return builder.build()
    }

    private suspend fun getDefaultPendingIntent() = PendingIntent.getActivity(
        context,
        0,
        openTransfersSectionIntentMapper(TransfersTab.PENDING_TAB),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Display a notification for upload progress
     *
     * @progress an Int between 0 and 100
     */
    private suspend fun showUploadProgressNotification(
        totalUploaded: Int,
        totalToUpload: Int,
        totalUploadedBytes: Long,
        totalUploadBytes: Long,
        progress: Int,
        areUploadsPaused: Boolean,
    ) {
        getDefaultPendingIntent()?.let { defaultPendingIntent ->
            val content = context.getString(
                if (areUploadsPaused)
                    R.string.upload_service_notification_paused
                else
                    R.string.upload_service_notification,
                totalUploaded,
                totalToUpload
            )
            val subText = stringWrapper.getProgressSize(totalUploadedBytes, totalUploadBytes)
            val actionIntent = (if (getFeatureFlagValueUseCase(AppFeatures.SettingsComposeUI)) {
                SettingsHomeActivity.getIntent(context, cameraUploadsTargetPreference).also {
                    it.putExtra(INTENT_EXTRA_KEY_SHOW_DISABLE_CU, true)
                }
            } else {
                Intent(context, SettingsActivity::class.java).apply {
                    putExtra(INITIAL_PREFERENCE, cameraUploadsTargetPreference.preferenceId)
                    putExtra(
                        NAVIGATE_TO_INITIAL_PREFERENCE,
                        cameraUploadsTargetPreference.requiresNavigation
                    )
                    putExtra(INTENT_EXTRA_KEY_SHOW_DISABLE_CU, true)
                }
            }).let {
                PendingIntent.getActivity(
                    context,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            val notification = createNotification(
                title = context.getString(R.string.section_photo_sync),
                content = content,
                subText = subText,
                intent = defaultPendingIntent,
                actionText = context.getString(R.string.verify_2fa_subtitle_diable_2fa),
                actionIntent = actionIntent,
                isAutoCancel = false,
                isOngoing = true,
                progress = progress,
            )
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     *  Display a notification for video compression progress
     *
     *  @progress an Int between 0 and 100
     */
    private suspend fun showVideoCompressionProgressNotification(
        progress: Int,
        currentFileIndex: Int,
        totalCount: Int,
    ) {
        getDefaultPendingIntent()?.let { defaultPendingIntent ->
            val content = context.getString(
                R.string.title_compress_video,
                currentFileIndex,
                totalCount
            )
            val notification = createNotification(
                title = context.getString(R.string.message_compress_video, "$progress%"),
                content = content,
                subText = content,
                intent = defaultPendingIntent,
                isAutoCancel = false,
                isOngoing = true,
                progress = progress,
            )
            notificationManager.notify(COMPRESSION_NOTIFICATION_ID, notification)
        }
    }

    /**
     *  Display a notification for checking files to upload
     */
    private suspend fun showCheckUploadsNotification() {
        getDefaultPendingIntent()?.let { defaultPendingIntent ->
            val notification = createNotification(
                title = context.getString(R.string.section_photo_sync),
                content = context.getString(R.string.settings_camera_notif_checking_title),
                intent = defaultPendingIntent,
                isAutoCancel = false,
                isOngoing = true,
            )
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     *  Display a notification in case the cloud storage does not have enough space
     */
    private fun showStorageOverQuotaNotification() {
        val notification = createNotification(
            title = context.getString(R.string.overquota_alert_title),
            content = context.getString(R.string.download_show_info),
            intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ManagerActivity::class.java).apply {
                    action = ACTION_OVER_QUOTA_STORAGE
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ),
        )
        notificationManager.notify(OVER_STORAGE_QUOTA_NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification in case the device does not have enough local storage
     *  for video compression
     */
    private fun showVideoCompressionOutOfSpaceNotification() {
        val notification = createNotification(
            title = context.getString(R.string.title_out_of_space),
            content = context.getString(R.string.message_out_of_space),
            intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ManagerActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ),
        )
        notificationManager.notify(NOT_ENOUGH_STORAGE_NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification in case the device does not have enough local storage
     *  for creating temporary files
     */
    private fun showNotEnoughStorageNotification() {
        val notification = createNotification(
            title = context.getString(R.string.title_out_of_space),
            content = context.getString(R.string.error_not_enough_free_space),
            intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ManagerActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ),
        )
        notificationManager.notify(NOT_ENOUGH_STORAGE_NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification in case an error happened during video compression
     */
    private suspend fun showVideoCompressionErrorNotification() {
        val notification = createNotification(
            title = context.getString(R.string.title_compression_size_over_limit),
            content = context.getString(
                R.string.message_compression_size_over_limit,
                context.getString(
                    R.string.label_file_size_mega_byte,
                    getVideoCompressionSizeLimitUseCase().toString()
                )
            ),
            intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ManagerActivity::class.java).apply {
                    action = ACTION_SHOW_SETTINGS
                },
                PendingIntent.FLAG_IMMUTABLE
            ),
        )
        notificationManager.notify(COMPRESSION_ERROR_NOTIFICATION_ID, notification)
    }

    /**
     * When Camera Uploads cannot launch due to the Folder being unavailable, display a Notification
     * to inform the User
     *
     * @param cameraUploadsFolderType
     */
    private fun showFolderUnavailableNotification(cameraUploadsFolderType: CameraUploadFolderType) {
        val (resId, notificationId) = when (cameraUploadsFolderType) {
            CameraUploadFolderType.Primary ->
                Pair(
                    R.string.camera_notif_primary_local_unavailable,
                    PRIMARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID
                )

            CameraUploadFolderType.Secondary ->
                Pair(
                    R.string.camera_notif_secondary_local_unavailable,
                    SECONDARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID
                )
        }

        val isShown = notificationManager.activeNotifications.any { it.id == notificationId }
        if (!isShown) {
            val notification = createNotification(
                title = context.getString(R.string.section_photo_sync),
                content = context.getString(resId),
                intent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, ManagerActivity::class.java).apply {
                        action = ACTION_SHOW_SETTINGS
                    },
                    PendingIntent.FLAG_IMMUTABLE
                ),
            )
            notificationManager.notify(notificationId, notification)
        }
    }

    /**
     *  Display a notification in case there is no Wi-Fi connection
     */
    private fun showNoWifiConnectionNotification() {
        val action = NotificationCompat.Action.Builder(
            R.drawable.mega_ic,
            context.getString(sharedR.string.camera_uploads_notification_action_allow_mobile_data),
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, SettingsCameraUploadsActivity::class.java).apply {
                    putExtra(INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT, true)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
        val notification = createNotification(
            title = context.getString(sharedR.string.camera_uploads_notification_title_paused_warning),
            content = context.getString(sharedR.string.camera_uploads_notification_content_no_wifi_connection),
            action = action,
        )
        notificationManager.notify(NO_WIFI_CONNECTION_NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification in case there is no Network connection
     */
    private fun showNoNetworkConnectionNotification() {
        val notification = createNotification(
            title = context.getString(sharedR.string.camera_uploads_notification_title_paused_warning),
            content = context.getString(sharedR.string.camera_uploads_notification_content_no_network_connection),
        )
        notificationManager.notify(NO_NETWORK_CONNECTION_NOTIFICATION_ID, notification)
    }

    /**
     * Get foregroundInfo for camera uploads worker
     */
    fun getForegroundInfo(): ForegroundInfo {
        val notification = createNotification(
            title = context.getString(R.string.section_photo_sync),
            content = context.getString(R.string.settings_camera_notif_initializing_title),
            intent = null,
            isAutoCancel = false,
            isOngoing = true,
        )
        return createForegroundInfo(notification)
    }

    /**
     * Create a [ForegroundInfo] based on [Notification]
     */
    private fun createForegroundInfo(notification: Notification) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification
            )
        }

    /**
     * Dismiss error notifications
     */
    private fun cancelAllNotifications() {
        with(notificationManager) {
            cancel(PRIMARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID)
            cancel(SECONDARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID)
            cancel(COMPRESSION_ERROR_NOTIFICATION_ID)
            cancel(NOT_ENOUGH_STORAGE_NOTIFICATION_ID)
            cancel(OVER_STORAGE_QUOTA_NOTIFICATION_ID)
            cancel(COMPRESSION_NOTIFICATION_ID)
            cancel(NO_WIFI_CONNECTION_NOTIFICATION_ID)
            cancel(NO_NETWORK_CONNECTION_NOTIFICATION_ID)
        }
    }

    /**
     * Dismiss progress notification
     */
    private fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Dismiss compression progress notification
     */
    private fun cancelCompressionNotification() {
        notificationManager.cancel(COMPRESSION_NOTIFICATION_ID)
    }

}
