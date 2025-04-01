package mega.privacy.android.domain.entity.chat.messages.pending

import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Save pending message request
 *
 * @property chatId
 * @property transferUniqueId
 * @property type
 * @property uploadTimestamp
 * @property state
 * @property tempIdKarere
 * @property videoDownSampled
 * @property uriPath
 * @property nodeHandle
 * @property fingerprint
 * @property name
 */
data class SavePendingMessageRequest(
    val chatId: Long,
    val transferUniqueId: Long,
    val type: Int,
    val uploadTimestamp: Long,
    val state: PendingMessageState,
    val tempIdKarere: Long,
    val videoDownSampled: String?,
    val uriPath: UriPath,
    val nodeHandle: Long,
    val fingerprint: String?,
    val name: String?,
)
