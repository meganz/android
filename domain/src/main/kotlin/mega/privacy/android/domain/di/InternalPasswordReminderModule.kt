package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.BlockPasswordReminder
import mega.privacy.android.domain.usecase.NotifyPasswordChecked
import mega.privacy.android.domain.usecase.SkipPasswordReminder

/**
 * Dagger module to handle password reminder dialog behavior in TestPasswordActivity
 */
@Module
@DisableInstallInCheck
object InternalPasswordReminderModule {
    /**
     * Provide use case to handle skip password reminder
     */
    @Provides
    fun providesSkipPasswordReminder(repository: AccountRepository): SkipPasswordReminder =
        SkipPasswordReminder(repository::skipPasswordReminderDialog)

    /**
     * Provide use case to handle block password reminder
     */
    @Provides
    fun providesBlockPasswordReminder(repository: AccountRepository): BlockPasswordReminder =
        BlockPasswordReminder(repository::blockPasswordReminderDialog)

    /**
     * Provide use case to handle when password reminder has been finished
     */
    @Provides
    fun providesNotifyPasswordChecked(repository: AccountRepository): NotifyPasswordChecked =
        NotifyPasswordChecked(repository::notifyPasswordChecked)
}