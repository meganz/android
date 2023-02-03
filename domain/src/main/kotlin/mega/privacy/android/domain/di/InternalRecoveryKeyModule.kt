package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetExportMasterKey
import mega.privacy.android.domain.usecase.SetMasterKeyExported

@Module
@DisableInstallInCheck
internal object InternalRecoveryKeyModule {
    /**
     * Provides GetExportMasterKey Use Case
     */
    @Provides
    fun providesExportMasterKey(repository: SettingsRepository): GetExportMasterKey =
        GetExportMasterKey(repository::getExportMasterKey)

    /**
     * Provides SetMasterKeyExported Use Case
     */
    @Provides
    fun providesSetMasterKeyExported(repository: SettingsRepository): SetMasterKeyExported =
        SetMasterKeyExported(repository::setMasterKeyExported)
}