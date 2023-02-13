package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.ChangePassword
import mega.privacy.android.domain.usecase.GetPasswordStrength
import mega.privacy.android.domain.usecase.IsCurrentPassword
import mega.privacy.android.domain.usecase.ResetPassword

@Module
@DisableInstallInCheck
internal object InternalChangePasswordModule {
    /**
     * Provides ChangePassword UseCase
     */
    @Provides
    fun providesExportMasterKey(repository: AccountRepository): ChangePassword =
        ChangePassword(repository::changePassword)

    /**
     * Provides GetPasswordStrength UseCase
     */
    @Provides
    fun provideGetPasswordStrength(repository: AccountRepository): GetPasswordStrength =
        GetPasswordStrength(repository::getPasswordStrength)

    /**
     * Provides IsCurrentPassword UseCase
     */
    @Provides
    fun provideIsCurrentPassword(repository: AccountRepository): IsCurrentPassword =
        IsCurrentPassword(repository::isCurrentPassword)

    /**
     * Provides GetPasswordStrength UseCase
     */
    @Provides
    fun provideResetPassword(repository: AccountRepository): ResetPassword =
        ResetPassword(repository::resetPasswordFromLink)
}