package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import mega.privacy.android.domain.usecase.GetMultiFactorAuthCode

@Module
@DisableInstallInCheck
internal object InternalTwoFactorAuthenticationModule {

    /**
     * Provides EnableMultiFactorAuth Use Case
     */
    @Provides
    fun providesEnableMultiFactorAuth(repository: SettingsRepository): EnableMultiFactorAuth =
        EnableMultiFactorAuth(repository::enableMultiFactorAuth)

    /**
     * Provides GetMultiFactorAuthCode Use Case
     */
    @Provides
    fun providesGetMultiFactorAuthCode(repository: SettingsRepository): GetMultiFactorAuthCode =
        GetMultiFactorAuthCode(repository::getMultiFactorAuthCode)

}