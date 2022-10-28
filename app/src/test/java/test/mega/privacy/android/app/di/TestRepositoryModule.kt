package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.RepositoryModule
import mega.privacy.android.data.repository.GlobalStatesRepository
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.LoginRepository
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [RepositoryModule::class],
    components = [SingletonComponent::class]
)
object TestRepositoryModule {

    @Provides
    fun provideCameraUploadRepository(): CameraUploadRepository = mock()

    @Provides
    fun bindGlobalUpdatesRepository(): GlobalStatesRepository = mock()

    @Provides
    fun provideLoginRepository(): LoginRepository = mock()
}