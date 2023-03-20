package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.account.PasswordReminderUseCases
import mega.privacy.android.domain.usecase.BlockPasswordReminder
import mega.privacy.android.domain.usecase.NotifyPasswordChecked
import mega.privacy.android.domain.usecase.SkipPasswordReminder
import org.mockito.kotlin.mock

@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [PasswordReminderUseCases::class]
)
@Module
object TestPasswordReminderModule {
    private val skipPasswordReminder: SkipPasswordReminder = mock()
    private val blockPasswordReminder: BlockPasswordReminder = mock()
    private val notifyPasswordChecked: NotifyPasswordChecked = mock()

    @Provides
    fun provideSkipPasswordReminder() = skipPasswordReminder

    @Provides
    fun provideBlockPasswordReminder() = blockPasswordReminder

    @Provides
    fun provideNotifyPasswordChecked() = notifyPasswordChecked
}