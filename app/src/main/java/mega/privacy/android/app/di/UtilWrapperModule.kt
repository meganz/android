package mega.privacy.android.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.jobservices.CameraUploadsServiceWrapper
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManagerWrapper
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.IsOnlineWrapper
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper

/**
 * Util wrapper module
 *
 * Module for providing temporary wrappers around static util methods. All of these dependencies
 * need to be removed during the refactoring process.
 */
@Module
@InstallIn(FragmentComponent::class, SingletonComponent::class)
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
}
