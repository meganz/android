package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.settings.download.DownloadSettingsUseCases
import mega.privacy.android.domain.usecase.GetStorageDownloadAskAlways
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPath
import mega.privacy.android.domain.usecase.GetStorageDownloadLocation
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlways
import mega.privacy.android.domain.usecase.SetStorageDownloadLocation
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [DownloadSettingsUseCases::class],
    components = [ViewModelComponent::class]
)
object TestDownloadSettingsUseCases {
    val getDownloadLocationPath = mock<GetStorageDownloadLocation>()
    val setDownloadLocationPath = mock<SetStorageDownloadLocation>()
    val getStorageDownloadAskAlways = mock<GetStorageDownloadAskAlways>()
    val setStorageDownloadAskAlways = mock<SetStorageDownloadAskAlways>()
    val getDefaultDownloadPath = mock<GetStorageDownloadDefaultPath>()

    @Provides
    fun provideDefaultDownloadLocationPath(): GetStorageDownloadDefaultPath = getDefaultDownloadPath

    @Provides
    fun provideGetDownloadLocationPath(): GetStorageDownloadLocation = getDownloadLocationPath

    @Provides
    fun provideSetDownloadLocationPath(): SetStorageDownloadLocation = setDownloadLocationPath

    @Provides
    fun provideGetStorageAskAlways(): GetStorageDownloadAskAlways = getStorageDownloadAskAlways

    @Provides
    fun provideSetStorageAskAlways(): SetStorageDownloadAskAlways = setStorageDownloadAskAlways
}