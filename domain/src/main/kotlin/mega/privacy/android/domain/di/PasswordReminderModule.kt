package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

/**
 * Domain Dagger module for Use Cases to handle password reminder in TestPasswordActivity
 */
@Module(includes = [InternalPasswordReminderModule::class])
@DisableInstallInCheck
class PasswordReminderModule {}