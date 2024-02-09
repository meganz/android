package mega.privacy.android.domain.entity.meeting

/**
 *  Call notification type
 */
enum class CallNotificationType {
    /**
     *  Invalid notification type
     */
    Invalid,

    /**
     *  Error received from SFU
     */
    SFUError,

    /**
     *  Command denied by SFU
     */
    SFUDeny,
}