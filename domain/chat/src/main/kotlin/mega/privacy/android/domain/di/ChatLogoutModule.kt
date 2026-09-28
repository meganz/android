package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.logout.ClearChatDataLogoutTask
import mega.privacy.android.domain.usecase.logout.LogoutTask

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ChatLogoutModule {
    companion object {
        @Provides
        @IntoSet
        fun provideClearChatDataLogoutTask(task: ClearChatDataLogoutTask): LogoutTask =
            task
    }
}
