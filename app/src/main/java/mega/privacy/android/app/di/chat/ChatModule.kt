package mega.privacy.android.app.di.chat

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.AnswerChatCall
import mega.privacy.android.domain.usecase.CheckChatLink
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.DefaultGetChatParticipants
import mega.privacy.android.domain.usecase.DefaultGetMeetings
import mega.privacy.android.domain.usecase.DefaultMeetingRoomMapper
import mega.privacy.android.domain.usecase.DefaultOpenOrStartCall
import mega.privacy.android.domain.usecase.GetChatCall
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetMeetings
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.InviteContact
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.MeetingRoomMapper
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.OpenOrStartCall
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveChatLink
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetMyChatFilesFolder
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
import mega.privacy.android.domain.usecase.UpdateChatPermissions
import mega.privacy.android.domain.usecase.meeting.FetchNumberOfScheduledMeetingOccurrencesByChat
import mega.privacy.android.domain.usecase.meeting.FetchScheduledMeetingOccurrencesByChat
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingOccurrencesUpdates
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRinging
import mega.privacy.android.domain.usecase.setting.ResetChatSettings

/**
 * Chats module.
 *
 * Provides all chats and calls implementation.
 */
@Module
@InstallIn(SingletonComponent::class, ViewModelComponent::class)
abstract class ChatModule {

    /**
     * Get chat participants
     */
    @Binds
    abstract fun bindGetChatParticipants(useCase: DefaultGetChatParticipants): GetChatParticipants

    /**
     * Get chat meetings
     */
    @Binds
    abstract fun bindGetMeetings(useCase: DefaultGetMeetings): GetMeetings

    /**
     * Meeting item room mapper
     */
    @Binds
    abstract fun bindMeetingRoomMapper(implementation: DefaultMeetingRoomMapper): MeetingRoomMapper

    /**
     * Open call or start call and open it
     */
    @Binds
    abstract fun bindOpenOrStartChatCall(useCase: DefaultOpenOrStartCall): OpenOrStartCall

    companion object {
        /**
         * Provides the Use Case [GetChatRoom]
         */
        @Provides
        fun provideGetChatRoom(chatRepository: ChatRepository): GetChatRoom =
            GetChatRoom(chatRepository::getChatRoom)

        /**
         * Provides the Use Case [GetChatCall]
         */
        @Provides
        fun provideGetChatCall(callRepository: CallRepository): GetChatCall =
            GetChatCall(callRepository::getChatCall)

        /**
         * Provides the Use Case [GetScheduledMeetingByChat]
         */
        @Provides
        fun provideGetScheduledMeetingByChat(chatRepository: ChatRepository): GetScheduledMeetingByChat =
            GetScheduledMeetingByChat(chatRepository::getScheduledMeetingsByChat)

        /**
         * Provides the Use Case [FetchScheduledMeetingOccurrencesByChat]
         */
        @Provides
        fun provideFetchScheduledMeetingOccurrencesByChat(chatRepository: ChatRepository): FetchScheduledMeetingOccurrencesByChat =
            FetchScheduledMeetingOccurrencesByChat(chatRepository::fetchScheduledMeetingOccurrencesByChat)

        /**
         * Provides the Use Case [FetchNumberOfScheduledMeetingOccurrencesByChat]
         */
        @Provides
        fun provideFetchNumberOfScheduledMeetingOccurrencesByChat(chatRepository: ChatRepository): FetchNumberOfScheduledMeetingOccurrencesByChat =
            FetchNumberOfScheduledMeetingOccurrencesByChat(chatRepository::fetchScheduledMeetingOccurrencesByChat)

        /**
         * Provides the Use Case [StartChatCallNoRinging]
         */
        @Provides
        fun provideStartChatCallNoRinging(callRepository: CallRepository): StartChatCallNoRinging =
            StartChatCallNoRinging(callRepository::startCallNoRinging)

        /**
         * Provides the Use Case [StartChatCall]
         */
        @Provides
        fun provideStartChatCall(callRepository: CallRepository): StartChatCall =
            StartChatCall(callRepository::startCallRinging)

        /**
         * Provides the Use Case [SetOpenInvite]
         */
        @Provides
        fun provideSetOpenInvite(chatRepository: ChatRepository): SetOpenInvite =
            SetOpenInvite(chatRepository::setOpenInvite)

        /**
         * Provides the Use Case [AnswerChatCall]
         */
        @Provides
        fun provideAnswerChatCall(callRepository: CallRepository): AnswerChatCall =
            AnswerChatCall(callRepository::answerChatCall)

        /**
         * Provides the Use Case [InviteToChat]
         */
        @Provides
        fun provideInviteToChat(chatRepository: ChatRepository): InviteToChat =
            InviteToChat(chatRepository::inviteToChat)

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
         * Provides the Use Case [CreateChatLink]
         */
        @Provides
        fun provideCreateChatLink(chatRepository: ChatRepository): CreateChatLink =
            CreateChatLink(chatRepository::createChatLink)

        /**
         * Provides the Use Case [RemoveChatLink]
         */
        @Provides
        fun provideRemoveChatLink(chatRepository: ChatRepository): RemoveChatLink =
            RemoveChatLink(chatRepository::removeChatLink)

        /**
         * Provides the Use Case [QueryChatLink]
         */
        @Provides
        fun provideQueryChatLink(chatRepository: ChatRepository): QueryChatLink =
            QueryChatLink(chatRepository::queryChatLink)

        /**
         * Provides the Use Case [CheckChatLink]
         */
        @Provides
        fun provideCheckChatLink(chatRepository: ChatRepository): CheckChatLink =
            CheckChatLink(chatRepository::checkChatLink)

        /**
         * Provides the Use Case [MonitorScheduledMeetingOccurrencesUpdates]
         */
        @Provides
        fun provideMonitorScheduledMeetingOccurrencesUpdates(chatRepository: ChatRepository): MonitorScheduledMeetingOccurrencesUpdates =
            MonitorScheduledMeetingOccurrencesUpdates(chatRepository::monitorScheduledMeetingOccurrencesUpdates)

        /**
         * Provides the Use Case [MonitorScheduledMeetingUpdates]
         */
        @Provides
        fun provideMonitorScheduledMeetingUpdates(chatRepository: ChatRepository): MonitorScheduledMeetingUpdates =
            MonitorScheduledMeetingUpdates(chatRepository::monitorScheduledMeetingUpdates)


        /**
         * Provides the Use Case [MonitorChatRoomUpdates]
         */
        @Provides
        fun provideMonitorChatRoomUpdates(chatRepository: ChatRepository): MonitorChatRoomUpdates =
            MonitorChatRoomUpdates(chatRepository::monitorChatRoomUpdates)

        /**
         * Provides the Use Case [MonitorChatListItemUpdates]
         */
        @Provides
        fun provideMonitorChatListItemUpdates(chatRepository: ChatRepository): MonitorChatListItemUpdates =
            MonitorChatListItemUpdates(chatRepository::monitorChatListItemUpdates)

        /**
         * Provides the Use Case [UpdateChatPermissions]
         */
        @Provides
        fun provideUpdateChatPermissions(chatRepository: ChatRepository): UpdateChatPermissions =
            UpdateChatPermissions(chatRepository::updateChatPermissions)

        /**
         * Provides the Use Case [RemoveFromChat]
         */
        @Provides
        fun provideRemoveFromChat(chatRepository: ChatRepository): RemoveFromChat =
            RemoveFromChat(chatRepository::removeFromChat)

        /**
         * Provides the Use Case [InviteContact]
         */
        @Provides
        fun provideInviteContact(chatRepository: ChatRepository): InviteContact =
            InviteContact(chatRepository::inviteContact)

        /**
         * Provides the Use Case [SetMyChatFilesFolder]
         */
        @Provides
        fun provideSetMyChatFilesFolder(fileSystemRepository: FileSystemRepository): SetMyChatFilesFolder =
            SetMyChatFilesFolder(fileSystemRepository::setMyChatFilesFolder)

        /**
         * Provides the Use case [ResetChatSettings]
         */
        @Provides
        fun provideResetChatSettings(chatRepository: ChatRepository): ResetChatSettings =
            ResetChatSettings(chatRepository::resetChatSettings)
    }
}
