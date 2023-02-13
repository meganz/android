package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.settings.export.ExportRecoveryKeyUseCases
import mega.privacy.android.domain.usecase.GetExportMasterKey
import mega.privacy.android.domain.usecase.SetMasterKeyExported
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [ExportRecoveryKeyUseCases::class],
    components = [ViewModelComponent::class]
)
object TestExportRecoveryKeyUseCases {
    val getExportMasterKey = mock<GetExportMasterKey>()
    val setMasterKeyExported = mock<SetMasterKeyExported>()

    @Provides
    fun provideGetExportMasterKey(): GetExportMasterKey = getExportMasterKey

    @Provides
    fun provideSetMasterKeyExported(): SetMasterKeyExported = setMasterKeyExported
}