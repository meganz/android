package mega.privacy.android.app.di

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.domain.usecase.DefaultGetNodeLocationInfo
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.permission.PermissionUtilWrapperImpl
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.app.utils.wrapper.GetFullPathFileWrapper
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilFacade
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.wrapper.ApplicationIpAddressWrapper
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.CameraUploadSyncManagerWrapper
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.BackupState

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
            }

        /**
         * provide default implementation for FileUtilWrapper
         */
        @Provides
        fun providesFileUtilWrapper() = object : FileUtilWrapper {}

        /**
         * Provides the [StringWrapper]
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

    }
}
