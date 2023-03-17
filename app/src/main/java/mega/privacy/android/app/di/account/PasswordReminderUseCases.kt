package mega.privacy.android.app.di.account

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.di.PasswordReminderModule

/**
 * Dagger module for Use Cases to handle password reminder in TestPasswordActivity
 */
@Module(includes = [PasswordReminderModule::class])
@InstallIn(ViewModelComponent::class)
class PasswordReminderUseCases {}