package mega.privacy.android.app.di.chat

import android.app.NotificationManager
import androidx.core.app.NotificationChannelCompat
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.DefaultGetChatParticipants
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetMyChatFilesFolder
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.meeting.FetchNumberOfScheduledMeetingOccurrencesByChat
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeeting

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


    companion object {

        /**
         * Provides the Use Case [GetScheduledMeeting]
         */
        @Provides
        fun provideGetScheduledMeeting(callRepository: CallRepository): GetScheduledMeeting =
            GetScheduledMeeting(callRepository::getScheduledMeeting)

        /**
         * Provides the Use Case [FetchNumberOfScheduledMeetingOccurrencesByChat]
         */
        @Provides
        fun provideFetchNumberOfScheduledMeetingOccurrencesByChat(callRepository: CallRepository): FetchNumberOfScheduledMeetingOccurrencesByChat =
            FetchNumberOfScheduledMeetingOccurrencesByChat(callRepository::fetchScheduledMeetingOccurrencesByChat)

        /**
         * Provides the Use Case [InviteToChat]
         */
        @Provides
        fun provideInviteToChat(chatRepository: ChatRepository): InviteToChat =
            InviteToChat(chatRepository::inviteToChat)

        /**
         * Provides the Use Case [SetPublicChatToPrivate]
         */
        @Provides
        fun provideSetPublicChatToPrivate(chatRepository: ChatRepository): SetPublicChatToPrivate =
            SetPublicChatToPrivate(chatRepository::setPublicChatToPrivate)

        /**
         * Provides the Use Case [MonitorChatListItemUpdates]
         */
        @Provides
        fun provideMonitorChatListItemUpdates(chatRepository: ChatRepository): MonitorChatListItemUpdates =
            MonitorChatListItemUpdates(chatRepository::monitorChatListItemUpdates)

        /**
         * Provides the Use Case [RemoveFromChat]
         */
        @Provides
        fun provideRemoveFromChat(chatRepository: ChatRepository): RemoveFromChat =
            RemoveFromChat(chatRepository::removeFromChat)

        /**
         * Provides the Use Case [SetMyChatFilesFolder]
         */
        @Provides
        fun provideSetMyChatFilesFolder(fileSystemRepository: FileSystemRepository): SetMyChatFilesFolder =
            SetMyChatFilesFolder(fileSystemRepository::setMyChatFilesFolder)

        /**
         * Provides the Use case [SignalChatPresenceActivity]
         */
        @Provides
        fun provideSignalChatPresenceActivity(chatRepository: ChatRepository): SignalChatPresenceActivity =
            SignalChatPresenceActivity(chatRepository::signalPresenceActivity)

        /**
         * Provides chat notification channel
         */
        @Provides
        @IntoSet
        fun provideChatNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_CHAT_ID,
                NotificationManager.IMPORTANCE_HIGH
            ).setName(Constants.NOTIFICATION_CHANNEL_CHAT_NAME).build()

        /**
         * Provides chat summary notification channel
         */
        @Provides
        @IntoSet
        fun provideChatSummaryNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_ID_V2,
                NotificationManager.IMPORTANCE_HIGH
            ).setName(Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_NAME)
                .setShowBadge(true)
                .setVibrationEnabled(true)
                .setVibrationPattern(longArrayOf(0, 500))
                .setLightsEnabled(true)
                .setLightColor(android.graphics.Color.rgb(0, 255, 0))
                .build()

        /**
         * Provides chat summary without vibration notification channel
         */
        @Provides
        @IntoSet
        fun provideChatSummaryNoVibrationNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_ID,
                NotificationManager.IMPORTANCE_HIGH
            ).setName(Constants.NOTIFICATION_CHANNEL_CHAT_SUMMARY_NO_VIBRATE_NAME)
                .build()

        /**
         * Provides incoming calls notification channel
         */
        @Provides
        @IntoSet
        fun provideIncomingCallsNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_ID,
                NotificationManager.IMPORTANCE_HIGH
            ).setName(Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_NAME)
                .setLightsEnabled(true)
                .setVibrationEnabled(true)
                .setVibrationPattern(longArrayOf(0, 1000, 1000, 1000, 1000, 1000, 1000))
                .build()

        /**
         * Provides incoming calls without notification channel
         */
        @Provides
        @IntoSet
        fun provideIncomingCallsNoVibrationNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_ID,
                NotificationManager.IMPORTANCE_HIGH
            ).setName(Constants.NOTIFICATION_CHANNEL_INCOMING_CALLS_NO_VIBRATE_NAME).build()

        /**
         * Provides in progress and missed calls notification channel
         */
        @Provides
        @IntoSet
        fun provideInProgressMissedCallsNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_ID,
                NotificationManager.IMPORTANCE_HIGH
            ).setName(Constants.NOTIFICATION_CHANNEL_INPROGRESS_MISSED_CALLS_NAME).build()


    }
}
