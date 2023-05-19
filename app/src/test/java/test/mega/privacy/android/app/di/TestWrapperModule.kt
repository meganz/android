package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.UtilWrapperModule
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import mega.privacy.android.app.utils.wrapper.GetDocumentFileWrapper
import mega.privacy.android.app.utils.wrapper.GetFullPathFileWrapper
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.app.utils.wrapper.TimeWrapper
import mega.privacy.android.data.wrapper.AvatarWrapper
import mega.privacy.android.data.wrapper.CameraUploadSyncManagerWrapper
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UtilWrapperModule::class]
)
object TestWrapperModule {

    val getFullPathWrapper = mock<GetFullPathFileWrapper>()
    val getDocumentFileWrapper = mock<GetDocumentFileWrapper>()
    val cameraUploadSyncManagerWrapper = mock<CameraUploadSyncManagerWrapper>()
    val permissionUtilWrapper = mock<PermissionUtilWrapper>()
    val getOfflineThumbnailFileWrapper = mock<GetOfflineThumbnailFileWrapper>()
    val fetchNodeWrapper = mock<FetchNodeWrapper>()
    val timeWrapper = mock<TimeWrapper>()
    val avatarWrapper = mock<AvatarWrapper>()

    @Provides
    fun provideGetFullPathFileWrapper(): GetFullPathFileWrapper = getFullPathWrapper

    @Provides
    fun provideGetDocumentFileWrapper(): GetDocumentFileWrapper = getDocumentFileWrapper

    @Provides
    fun provideCameraUploadSyncManagerWrapper(): CameraUploadSyncManagerWrapper =
        cameraUploadSyncManagerWrapper

    @Provides
    fun providePermissionUtilWrapper(): PermissionUtilWrapper = permissionUtilWrapper

    @Provides
    fun provideGetOfflineThumbnailFileWrapper(): GetOfflineThumbnailFileWrapper =
        getOfflineThumbnailFileWrapper


    @Provides
    fun provideFetchNodeWrapper(): FetchNodeWrapper = fetchNodeWrapper

    @Provides
    fun provideTimeWrapper(): TimeWrapper = timeWrapper

    @Provides
    fun provideAvatarWrapper(): AvatarWrapper = avatarWrapper

    @Provides
    fun provideMegaNodeUtilWrapper(): MegaNodeUtilWrapper = mock()

    @Provides
    fun providesFileUtilWrapper(): FileUtilWrapper = mock()
}
