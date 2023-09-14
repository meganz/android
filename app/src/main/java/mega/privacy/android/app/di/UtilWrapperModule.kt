package mega.privacy.android.app.di

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.DefaultGetNodeLocationInfo
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.notifications.CameraUploadsNotificationManager
import mega.privacy.android.app.presentation.extensions.getErrorStringId
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.permission.PermissionUtilWrapperImpl
import mega.privacy.android.app.utils.wrapper.CameraEnumeratorWrapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.app.utils.wrapper.GetFullPathFileWrapper
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilFacade
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.app.utils.wrapper.SetLogoutFlagWrapperImpl
import mega.privacy.android.data.facade.security.SetLogoutFlagWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.wrapper.ApplicationIpAddressWrapper
import mega.privacy.android.data.wrapper.ApplicationWrapper
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.CameraUploadSyncManagerWrapper
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.feature.sync.data.service.ApplicationLoggingInSetter
import org.webrtc.Camera1Enumerator
import org.webrtc.CameraEnumerator

/**
 * Util wrapper module
 *
 * Module for providing temporary wrappers around static util methods. All of these dependencies
 * need to be removed during the refactoring process.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UtilWrapperModule {

    /**
     * Bind mega node util wrapper
     */
    @Binds
    abstract fun bindMegaNodeUtilWrapper(implementation: MegaNodeUtilFacade): MegaNodeUtilWrapper

    @Binds
    abstract fun bindPermissionUtilWrapper(implementation: PermissionUtilWrapperImpl): PermissionUtilWrapper

    @Binds
    abstract fun bindGetNodeLocation(implementation: DefaultGetNodeLocationInfo): GetNodeLocationInfo

    @Binds
    abstract fun bindSetLogoutFlagWrapper(implementation: SetLogoutFlagWrapperImpl): SetLogoutFlagWrapper


    companion object {

        @Provides
        fun provideGetFullPathFileWrapper(): GetFullPathFileWrapper {
            return object : GetFullPathFileWrapper {
                override fun getFullPathFromTreeUri(uri: Uri, context: Context): String? {
                    return FileUtil.getFullPathFromTreeUri(uri, context)
                }
            }
        }

        @Provides
        fun provideCameraUploadSyncManagerWrapper(): CameraUploadSyncManagerWrapper =
            object : CameraUploadSyncManagerWrapper {
                override fun doRegularHeartbeat() = CameraUploadSyncManager.doRegularHeartbeat()

                override fun updatePrimaryFolderBackupState(backupState: BackupState) =
                    CameraUploadSyncManager.updatePrimaryFolderBackupState(backupState)

                override fun updateSecondaryFolderBackupState(backupState: BackupState) =
                    CameraUploadSyncManager.updateSecondaryFolderBackupState(backupState)
            }

        @Provides
        fun provideFetchNodeWrapper(megaApiGateway: MegaApiGateway): FetchNodeWrapper =
            FetchNodeWrapper(megaApiGateway::getMegaNodeByHandle)

        @Provides
        fun provideGetOfflineThumbnailFileWrapper(megaApiGateway: MegaApiGateway): GetOfflineThumbnailFileWrapper {
            return object : GetOfflineThumbnailFileWrapper {
                override fun getThumbnailFile(context: Context, node: MegaOffline) =
                    OfflineUtils.getThumbnailFile(context, node, megaApiGateway)

                override fun getThumbnailFile(context: Context, handle: String) =
                    OfflineUtils.getThumbnailFile(context, handle, megaApiGateway)
            }
        }

        /**
         * provide Avatar Wrapper
         */
        @Provides
        fun provideAvatarWrapper() = object : AvatarWrapper {
            override fun getDominantColor(bimap: Bitmap): Int = AvatarUtil.getDominantColor(bimap)

            override fun getSpecificAvatarColor(typeColor: String): Int =
                AvatarUtil.getSpecificAvatarColor(typeColor)

            override fun getFirstLetter(name: String): String =
                AvatarUtil.getFirstLetter(name)
        }


        /**
         * Provides the [StringWrapper]
         */
        @Provides
        fun provideStringWrapper(@ApplicationContext context: Context) =
            object : StringWrapper {
                override fun getProgressSize(progress: Long, size: Long): String =
                    Util.getProgressSize(context, progress, size)

                override fun encodeBase64(string: String): String =
                    Base64.encodeToString(string.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

                override fun decodeBase64(base64: String): String =
                    try {
                        Base64.decode(base64.trim(), Base64.DEFAULT).toString(Charsets.UTF_8)
                    } catch (ignore: IllegalArgumentException) {
                        Base64.decode(base64.trim(), Base64.URL_SAFE).toString(Charsets.UTF_8)
                    }

                override fun getSizeString(size: Long) = Util.getSizeString(size, context)

                override fun getErrorStringResource(megaException: MegaException) =
                    context.getString(megaException.getErrorStringId())

                override fun getCloudDriveSection() =
                    context.getString(R.string.section_cloud_drive)

                override fun getRubbishBinSection() =
                    context.getString(R.string.section_rubbish_bin)

                override fun getTitleIncomingSharesExplorer() =
                    context.getString(R.string.title_incoming_shares_explorer)

                override fun getErrorStorageQuota() =
                    context.getString(R.string.error_share_owner_storage_quota)

                override fun getSavedForOfflineNew() =
                    context.getString(R.string.section_saved_for_offline_new)
            }

        /**
         * provide default implementation for FileUtilWrapper
         */
        @Provides
        fun providesFileUtilWrapper() = object : FileUtilWrapper {}

        /**
         * Provides the [ApplicationIpAddressWrapper]
         */
        @Provides
        fun provideApplicationIpAddressWrapper(application: Application) =
            object : ApplicationIpAddressWrapper {
                override fun setIpAddress(ipAddress: String?) {
                    (application as MegaApplication).localIpAddress = ipAddress
                }

                override fun getIpAddress(): String? {
                    return (application as MegaApplication).localIpAddress
                }
            }

        /**
         * Provides the [CameraUploadsNotificationManagerWrapper]
         */
        @Provides
        fun provideNotificationHelper(cameraUploadsNotificationManager: CameraUploadsNotificationManager) =
            object : CameraUploadsNotificationManagerWrapper {
                override fun getForegroundInfo() =
                    cameraUploadsNotificationManager.getForegroundInfo()

                override fun cancelNotifications() =
                    cameraUploadsNotificationManager.cancelAllNotifications()

                override fun cancelNotification() =
                    cameraUploadsNotificationManager.cancelNotification()

            }

        /**
         * Provides the [ApplicationWrapper]
         */
        @Provides
        fun provideApplicationWrapper() =
            object : ApplicationWrapper {
                override fun setLoggingIn(isLoggingIn: Boolean) {
                    MegaApplication.isLoggingIn = isLoggingIn
                }

                override fun isLoggingIn() = MegaApplication.isLoggingIn

                override fun setHeartBeatAlive(isAlive: Boolean) {
                    MegaApplication.setHeartBeatAlive(isAlive)
                }
            }

        /**
         * Provides the [ApplicationLoggingInSetter]
         */
        @Provides
        fun provideApplicationLoggingInSetter(): ApplicationLoggingInSetter =
            object : ApplicationLoggingInSetter {
                override fun setLoggingIn(loggingIn: Boolean) {
                    MegaApplication.isLoggingIn = loggingIn
                }

                override fun isLoggingIn(): Boolean = MegaApplication.isLoggingIn
            }

        /**
         * Provides the [CookieEnabledCheckWrapper]
         */
        @Provides
        fun provideCookieEnabledCheckWrapper(): CookieEnabledCheckWrapper =
            object : CookieEnabledCheckWrapper {
                override fun checkEnabledCookies() {
                    MegaApplication.getInstance().checkEnabledCookies()
                }
            }

        /**
         * Provides [CameraEnumeratorWrapper]
         */
        @Provides
        fun provideCameraEnumeratorWrapper(): CameraEnumeratorWrapper =
            object : CameraEnumeratorWrapper {
                override fun invoke(): CameraEnumerator =
                    Camera1Enumerator(true)
            }
    }
}
