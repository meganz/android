package mega.privacy.android.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.jobservices.CameraUploadsServiceWrapper
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManagerWrapper
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.app.utils.wrapper.IsOnlineWrapper
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import java.io.File

/**
 * Util wrapper module
 *
 * Module for providing temporary wrappers around static util methods. All of these dependencies
 * need to be removed during the refactoring process.
 */
@Module
@InstallIn(FragmentComponent::class, SingletonComponent::class, ServiceComponent::class)
class UtilWrapperModule {

    @Provides
    fun provideIsOnlineWrapper(): IsOnlineWrapper {
        return object : IsOnlineWrapper {
            override fun isOnline(context: Context): Boolean {
                return Util.isOnline(context)
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
    fun provideGetOfflineThumbnailFileWrapper(megaApiGateway: MegaApiGateway): GetOfflineThumbnailFileWrapper {
        return object : GetOfflineThumbnailFileWrapper {
            override fun getThumbnailFile(context: Context, node: MegaOffline): File {
                return OfflineUtils.getThumbnailFile(context, node, megaApiGateway)
            }

            override fun getThumbnailFile(context: Context, handle: String): File {
                return OfflineUtils.getThumbnailFile(context, handle, megaApiGateway)
            }
        }
    }
}
