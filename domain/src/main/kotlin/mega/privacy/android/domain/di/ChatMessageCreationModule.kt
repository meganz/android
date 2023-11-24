package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.usecase.chat.message.CreateAlterParticipantsMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateCallEndedMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateCallStartedMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateChatLinkCreatedMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateChatLinkRemovedMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateContactAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateMetaMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateNodeAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateNormalChatMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreatePermissionChangeMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreatePrivateModeSetMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateRetentionTimeUpdatedMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateRevokeNodeAttachmentMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateScheduledMeetingUpdatedMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateTitleChangeMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateTruncateHistoryMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateTypedMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.CreateVoiceClipMessageUseCase

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ChatMessageCreationModule {

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.ALTER_PARTICIPANTS)
    abstract fun bindCreateAlterParticipantsMessageUseCase(creator: CreateAlterParticipantsMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.CALL_ENDED)
    abstract fun bindCreateCallEndedMessageUseCase(creator: CreateCallEndedMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.CALL_STARTED)
    abstract fun bindCreateCallStartedMessageUseCase(creator: CreateCallStartedMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.PUBLIC_HANDLE_CREATE)
    abstract fun bindCreateChatLinkCreatedMessageUseCase(creator: CreateChatLinkCreatedMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.PUBLIC_HANDLE_DELETE)
    abstract fun bindCreateChatLinkDeletedMessageUseCase(creator: CreateChatLinkRemovedMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.CONTACT_ATTACHMENT)
    abstract fun bindCreateContactAttachmentMessageUseCase(creator: CreateContactAttachmentMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.CONTAINS_META)
    abstract fun bindCreateMetaMessageUseCase(creator: CreateMetaMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.NODE_ATTACHMENT)
    abstract fun bindCreateNodeAttachmentMessageUseCase(creator: CreateNodeAttachmentMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.NORMAL)
    abstract fun bindCreateNormalChatMessageUseCase(creator: CreateNormalChatMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.PRIV_CHANGE)
    abstract fun bindCreatePermissionChangeMessageUseCase(creator: CreatePermissionChangeMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.SET_PRIVATE_MODE)
    abstract fun bindCreatePrivateModeSetMessageUseCase(creator: CreatePrivateModeSetMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.SET_RETENTION_TIME)
    abstract fun bindCreateRetentionTimeUpdatedMessageUseCase(creator: CreateRetentionTimeUpdatedMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.REVOKE_NODE_ATTACHMENT)
    abstract fun bindCreateRevokeNodeAttachmentMessageUseCase(creator: CreateRevokeNodeAttachmentMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.SCHED_MEETING)
    abstract fun bindCreateScheduledMeetingUpdatedMessageUseCase(creator: CreateScheduledMeetingUpdatedMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.CHAT_TITLE)
    abstract fun bindCreateTitleChangeMessageUseCase(creator: CreateTitleChangeMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.TRUNCATE)
    abstract fun bindCreateTruncateHistoryMessageUseCase(creator: CreateTruncateHistoryMessageUseCase): CreateTypedMessageUseCase

    @Binds
    @IntoMap
    @ChatMessageTypeKey(ChatMessageType.VOICE_CLIP)
    abstract fun bindCreateVoiceClipMessageUseCase(creator: CreateVoiceClipMessageUseCase): CreateTypedMessageUseCase
}

