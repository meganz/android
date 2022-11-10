package mega.privacy.android.app.di

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.jobservices.CameraUploadsServiceWrapper
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManagerWrapper
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.GetDocumentFileWrapper
import mega.privacy.android.app.utils.wrapper.GetFullPathFileWrapper
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.app.utils.wrapper.IsOnWifiWrapper
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilFacade
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.app.utils.wrapper.TimeWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.wrapper.AvatarWrapper

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

    companion object {

        @Provides
        fun provideIsOnWifiWrapper(): IsOnWifiWrapper {
            return object : IsOnWifiWrapper {
                override fun isOnWifi(context: Context): Boolean {
                    return Util.isOnWifi(context)
                }
            }
        }

        @Provides
        fun provideGetFullPathFileWrapper(): GetFullPathFileWrapper {
            return object : GetFullPathFileWrapper {
                override fun getFullPathFromTreeUri(uri: Uri, context: Context): String? {
                    return FileUtil.getFullPathFromTreeUri(uri, context)
                }
            }
        }

        @Provides
        fun provideGetDocumentFileWrapper(): GetDocumentFileWrapper {
            return object : GetDocumentFileWrapper {
                override fun getDocumentFileFromTreeUri(context: Context, uri: Uri): DocumentFile? {
                    return DocumentFile.fromTreeUri(context, uri)
                }
            }
        }

        @Provides
        fun provideCameraUploadSyncManagerWrapper(): CameraUploadSyncManagerWrapper =
            object : CameraUploadSyncManagerWrapper {}

        @Provides
        fun provideCameraUploadsServiceWrapper(): CameraUploadsServiceWrapper =
            object : CameraUploadsServiceWrapper {}

        @Provides
        fun provideJobUtilWrapper(): JobUtilWrapper =
            object : JobUtilWrapper {}

        @Provides
        fun providePermissionUtilWrapper(): PermissionUtilWrapper =
            object : PermissionUtilWrapper {}

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
         * provide time manager
         */
        @Provides
        fun provideTimeWrapper() = object : TimeWrapper {
            override val now: Long
                get() = System.currentTimeMillis()

            override val nanoTime: Long
                get() = System.nanoTime()
        }

        /**
         * provide Avatar Wrapper
         */
        @Provides
        fun provideAvatarWrapper() = object : AvatarWrapper {
            override fun getDominantColor(bimap: Bitmap): Int = AvatarUtil.getDominantColor(bimap)

            override fun getSpecificAvatarColor(typeColor: String): Int =
                AvatarUtil.getSpecificAvatarColor(typeColor)
        }
    }
}
