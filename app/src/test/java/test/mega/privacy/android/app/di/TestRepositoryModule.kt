package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.RepositoryModule
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.repository.GlobalStatesRepository
import mega.privacy.android.app.domain.repository.TransfersRepository
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AlbumsRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.FeatureFlagRepository
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.PushesRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.repository.StatisticsRepository
import mega.privacy.android.domain.repository.SupportRepository
import mega.privacy.android.domain.repository.TransferRepository
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [RepositoryModule::class],
    components = [SingletonComponent::class]
)
object TestRepositoryModule {

    @Provides
    fun provideSettingsRepository(): SettingsRepository = mock()

    @Provides
    fun provideAccountRepository(): AccountRepository = mock()

    @Provides
    fun provideNetworkRepository(): NetworkRepository = mock()

    @Provides
    fun provideChatRepository(): ChatRepository = mock()

    @Provides
    fun provideCameraUploadRepository(): CameraUploadRepository = mock()

    @Provides
    fun provideFilesRepository(): FilesRepository = mock()

    @Provides
    fun bindFavouritesRepository(): FavouritesRepository = mock()

    @Provides
    fun bindLoggingRepository(): LoggingRepository = mock()

    @Provides
    fun bindAlbumsRepository(): AlbumsRepository = mock()

    @Provides
    fun bindGlobalUpdatesRepository(): GlobalStatesRepository = mock()

    @Provides
    fun bindSupportRepository(): SupportRepository = mock()

    @Provides
    fun bindDeviceRepository(): EnvironmentRepository = mock()

    @Provides
    fun provideContactsRepository(): NotificationsRepository = mock()

    @Provides
    fun providePushesRepository(): PushesRepository = mock()

    @Provides
    fun provideLoginRepository(): LoginRepository = mock()

    @Provides
    fun provideTransfersRepository(): TransfersRepository = mock()

    @Provides
    fun provideFeatureFlagRepository(): FeatureFlagRepository = mock()

    @Provides
    fun provideStatisticsRepository(): StatisticsRepository = mock()

    @Provides
    fun provideTransferRepository(): TransferRepository = mock()

    @Provides
    fun provideAvatarRepository(): AvatarRepository = mock()
}