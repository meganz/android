package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.chat.message.retry.RetryMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.retry.RetryPendingMessageUseCase

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RetryMessageModule {
    @Binds
    @IntoSet
    abstract fun bindRetryPendingMessageUseCase(creator: RetryPendingMessageUseCase): RetryMessageUseCase

    //other use cases will be added here, like AND-18405-Allow retry on error sending an edit for a message
}