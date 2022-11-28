package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.AnswerChatCall
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveChatLink
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
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

    /**
     * Provides the Use Case [LeaveChat]
     */
    @Provides
    fun provideLeaveChat(chatRepository: ChatRepository): LeaveChat =
        LeaveChat(chatRepository::leaveChat)

    /**
     * Provides the Use Case [SetPublicChatToPrivate]
     */
    @Provides
    fun provideSetPublicChatToPrivate(chatRepository: ChatRepository): SetPublicChatToPrivate =
        SetPublicChatToPrivate(chatRepository::setPublicChatToPrivate)

    /**
     * Provides the Use Case [QueryChatLink]
     */
    @Provides
    fun provideQueryChatLink(chatRepository: ChatRepository): QueryChatLink =
        QueryChatLink(chatRepository::queryChatLink)

    /**
     * Provides the Use Case [RemoveChatLink]
     */
    @Provides
    fun provideRemoveChatLink(chatRepository: ChatRepository): RemoveChatLink =
        RemoveChatLink(chatRepository::removeChatLink)

    /**
     * Provides the Use Case [CreateChatLink]
     */
    @Provides
    fun provideCreateChatLink(chatRepository: ChatRepository): CreateChatLink =
        CreateChatLink(chatRepository::createChatLink)

    /**
     * Provides the Use Case [MonitorChatListItemUpdates]
     */
    @Provides
    fun provideMonitorChatListItemUpdates(chatRepository: ChatRepository): MonitorChatListItemUpdates =
        MonitorChatListItemUpdates(chatRepository::monitorChatListItemUpdates)

}