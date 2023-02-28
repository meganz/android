package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import mega.privacy.android.domain.usecase.IsMasterKeyExported

@Module
@DisableInstallInCheck
internal object InternalTwoFactorAuthenticationModule {
    /**
     * Provides IsMasterKeyExported Use Case
     */
    @Provides
    fun providesIsMasterKeyExported(repository: SettingsRepository): IsMasterKeyExported =
        IsMasterKeyExported(repository::isMasterKeyExported)

    /**
     * Provides EnableMultiFactorAuth Use Case
     */
    @Provides
    fun providesEnableMultiFactorAuth(repository: SettingsRepository): EnableMultiFactorAuth =
        EnableMultiFactorAuth(repository::enableMultiFactorAuth)

}