package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.AnswerChatCall
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.MonitorScheduledMeetingUpdates
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

    /**
     * Provides the Use Case [SetOpenInvite]
     */
    @Provides
    fun provideSetOpenInvite(chatRepository: ChatRepository): SetOpenInvite =
        SetOpenInvite(chatRepository::setOpenInvite)

    /**
     * Provides the Use Case [StartChatCall]
     */
    @Provides
    fun provideStartChatCall(chatRepository: ChatRepository): StartChatCall =
        StartChatCall(chatRepository::startChatCall)

    /**
     * Provides the Use Case [AnswerChatCall]
     */
    @Provides
    fun provideAnswerChatCall(chatRepository: ChatRepository): AnswerChatCall =
        AnswerChatCall(chatRepository::answerChatCall)

    /**
     * Provides the Use Case [GetChatRoom]
     */
    @Provides
    fun provideGetChatRoom(chatRepository: ChatRepository): GetChatRoom =
        GetChatRoom(chatRepository::getChatRoom)

    /**
     * Provides the Use Case [MonitorChatRoomUpdates]
     */
    @Provides
    fun provideMonitorChatRoomUpdates(chatRepository: ChatRepository): MonitorChatRoomUpdates =
        MonitorChatRoomUpdates(chatRepository::monitorChatRoomUpdates)

    /**
     * Provides the Use Case [InviteToChat]
     */
    @Provides
    fun provideInviteToChat(chatRepository: ChatRepository): InviteToChat =
        InviteToChat(chatRepository::inviteToChat)

    /**
     * Provides the Use Case [MonitorScheduledMeetingUpdates]
     */
    @Provides
    fun provideMonitorScheduledMeetingUpdates(chatRepository: ChatRepository): MonitorScheduledMeetingUpdates =
        MonitorScheduledMeetingUpdates(chatRepository::monitorScheduledMeetingsUpdates)

    /**
     * Provides the Use Case [GetScheduledMeetingByChat]
     */
    @Provides
    fun provideGetScheduledMeetingByChat(chatRepository: ChatRepository): GetScheduledMeetingByChat =
        GetScheduledMeetingByChat(chatRepository::getScheduledMeetingsByChat)
}