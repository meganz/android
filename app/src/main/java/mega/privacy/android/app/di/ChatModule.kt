package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.SetOpenInvite

/**
 * Chats module.
 *
 * Provides all chats and calls implementation.
 */
@Module
@InstallIn(ViewModelComponent::class)
class ChatModule {

    @Provides
    fun provideSetOpenInvite(chatRepository: ChatRepository): SetOpenInvite =
        SetOpenInvite(chatRepository::setOpenInvite)
}