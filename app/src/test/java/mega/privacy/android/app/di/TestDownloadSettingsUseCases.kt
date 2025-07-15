package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.settings.download.DownloadSettingsUseCases
import mega.privacy.android.domain.usecase.GetDownloadLocationUseCase
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPathUseCase
import mega.privacy.android.domain.usecase.SetAskForDownloadLocationUseCase
import mega.privacy.android.domain.usecase.SetDownloadLocationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldPromptToSaveDestinationUseCase
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [DownloadSettingsUseCases::class],
    components = [ViewModelComponent::class]
)
object TestDownloadSettingsUseCases {
    val getDownloadLocationPath = mock<GetDownloadLocationUseCase>()
    val setDownloadLocationPath = mock<SetDownloadLocationUseCase>()
    val getStorageDownloadAskAlways = mock<ShouldPromptToSaveDestinationUseCase>()
    val setStorageDownloadAskAlways = mock<SetAskForDownloadLocationUseCase>()
    val getDefaultDownloadPath = mock<GetStorageDownloadDefaultPathUseCase>()

    @Provides
    fun provideDefaultDownloadLocationPath(): GetStorageDownloadDefaultPathUseCase =
        getDefaultDownloadPath

    @Provides
    fun provideGetDownloadLocationPath(): GetDownloadLocationUseCase =
        getDownloadLocationPath

    @Provides
    fun provideSetDownloadLocationPath(): SetDownloadLocationUseCase =
        setDownloadLocationPath

    @Provides
    fun provideGetStorageAskAlways(): ShouldPromptToSaveDestinationUseCase =
        getStorageDownloadAskAlways

    @Provides
    fun provideSetStorageAskAlways(): SetAskForDownloadLocationUseCase =
        setStorageDownloadAskAlways
}