package mega.privacy.android.domain.entity

/**
 * Chat room last message type
 */
enum class ChatRoomLastMessage {
    Unknown,
    Invalid,
    Normal,
    AlterParticipants,
    Truncate,
    PrivChange,
    ChatTitle,
    CallEnded,
    CallStarted,
    PublicHandleCreate,
    PublicHandleDelete,
    SetPrivateMode,
    SetRetentionTime,
    SchedMeeting,
    NodeAttachment,
    RevokeNodeAttachment,
    ContactAttachment,
    ContainsMeta,
    VoiceClip
}
