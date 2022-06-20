package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.RepositoryModule
import mega.privacy.android.app.domain.repository.*
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
    fun provideContactsRepository(): ContactsRepository = mock()

    @Provides
    fun providePushesRepository(): PushesRepository = mock()

    @Provides
    fun provideLoginRepository(): LoginRepository = mock()
}