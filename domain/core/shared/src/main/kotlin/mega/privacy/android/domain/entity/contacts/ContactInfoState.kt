package mega.privacy.android.domain.entity.contacts

/**
 * Aggregated live information about a contact or 1:1 chat peer.
 *
 * @property contactItem                       The contact, or null when the peer is not a
 *                                             contact (e.g. a non-contact 1:1 chat peer).
 * @property chatRoomId                        Id of the 1:1 chat room with the peer, or null
 *                                             when no chat room exists yet.
 * @property chatTitle                         Title of the 1:1 chat room the peer was resolved
 *                                             from, or null when resolved from a contact email.
 * @property userHandle                        Handle of the peer.
 * @property userChatStatus                    Current chat online status of the peer.
 * @property lastGreenMinutes                  Minutes since the peer was last seen online, or
 *                                             null when unknown.
 * @property isNotificationsMuted              True if the chat notifications are muted, false
 *                                             if not, or null when there is no chat room.
 * @property notificationsMutedUntilTimestamp  Timestamp (in seconds since the Epoch) until
 *                                             which the chat notifications are muted, or null
 *                                             when not muted, muted until turned back on, or
 *                                             there is no chat room.
 * @property retentionTimeSeconds              Retention time of the chat room in seconds, or
 *                                             null when disabled or there is no chat room.
 * @property inSharesCount                     Number of nodes the peer shares with the user.
 * @property hasOngoingCall                    True if there is an ongoing call in the chat room.
 */
data class ContactInfoState(
    val contactItem: ContactItem?,
    val chatRoomId: Long?,
    val chatTitle: String?,
    val userHandle: Long,
    val userChatStatus: UserChatStatus,
    val lastGreenMinutes: Int?,
    val isNotificationsMuted: Boolean?,
    val notificationsMutedUntilTimestamp: Long?,
    val retentionTimeSeconds: Long?,
    val inSharesCount: Int,
    val hasOngoingCall: Boolean,
)
