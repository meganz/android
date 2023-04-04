package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.settings.download.DownloadSettingsUseCases
import mega.privacy.android.domain.usecase.GetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPathUseCase
import mega.privacy.android.domain.usecase.GetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadLocationUseCase
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [DownloadSettingsUseCases::class],
    components = [ViewModelComponent::class]
)
object TestDownloadSettingsUseCases {
    val getDownloadLocationPath = mock<GetStorageDownloadLocationUseCase>()
    val setDownloadLocationPath = mock<SetStorageDownloadLocationUseCase>()
    val getStorageDownloadAskAlways = mock<GetStorageDownloadAskAlwaysUseCase>()
    val setStorageDownloadAskAlways = mock<SetStorageDownloadAskAlwaysUseCase>()
    val getDefaultDownloadPath = mock<GetStorageDownloadDefaultPathUseCase>()

    @Provides
    fun provideDefaultDownloadLocationPath(): GetStorageDownloadDefaultPathUseCase =
        getDefaultDownloadPath

    @Provides
    fun provideGetDownloadLocationPath(): GetStorageDownloadLocationUseCase =
        getDownloadLocationPath

    @Provides
    fun provideSetDownloadLocationPath(): SetStorageDownloadLocationUseCase =
        setDownloadLocationPath

    @Provides
    fun provideGetStorageAskAlways(): GetStorageDownloadAskAlwaysUseCase =
        getStorageDownloadAskAlways

    @Provides
    fun provideSetStorageAskAlways(): SetStorageDownloadAskAlwaysUseCase =
        setStorageDownloadAskAlways
}