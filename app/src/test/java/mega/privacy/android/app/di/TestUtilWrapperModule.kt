package mega.privacy.android.app.di

import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.UtilWrapperModule
import mega.privacy.android.app.utils.wrapper.CameraEnumeratorWrapper
import mega.privacy.android.data.wrapper.ApplicationWrapper
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [UtilWrapperModule::class],
    components = [SingletonComponent::class]
)
object TestUtilWrapperModule {

    @Provides
    fun provideNotificationHelper() = mock<CameraUploadsNotificationManagerWrapper>()

    @Provides
    fun provideApplicationWrapper() = mock<ApplicationWrapper>()

    @Provides
    fun provideCookieEnabledCheckWrapper() = mock<CookieEnabledCheckWrapper>()

    @Provides
    fun provideCameraEnumeratorWrapper() = mock<CameraEnumeratorWrapper>()
}
