package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetExportMasterKey
import mega.privacy.android.domain.usecase.IsMasterKeyExported
import mega.privacy.android.domain.usecase.SetMasterKeyExported

@Module
@DisableInstallInCheck
internal object InternalTwoFactorAuthenticationModule {
    /**
     * Provides IsMasterKeyExported Use Case
     */
    @Provides
    fun providesIsMasterKeyExported(repository: SettingsRepository): IsMasterKeyExported =
        IsMasterKeyExported(repository::isMasterKeyExported)

}