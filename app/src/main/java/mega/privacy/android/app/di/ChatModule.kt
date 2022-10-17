package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.StartChatCall

/**
 * Chats module.
 *
 * Provides all chats and calls implementation.
 */
@Module
@InstallIn(SingletonComponent::class, ViewModelComponent::class)
class ChatModule {

    @Provides
    fun provideSetOpenInvite(chatRepository: ChatRepository): SetOpenInvite =
        SetOpenInvite(chatRepository::setOpenInvite)

    @Provides
    fun provideStartChatCall(chatRepository: ChatRepository): StartChatCall =
        StartChatCall(chatRepository::startChatCall)
}