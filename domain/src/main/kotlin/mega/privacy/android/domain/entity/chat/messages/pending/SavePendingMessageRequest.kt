package mega.privacy.android.domain.entity.chat.messages.pending

import mega.privacy.android.domain.entity.chat.PendingMessageState

/**
 * Save pending message request
 *
 * @property chatId
 * @property type
 * @property uploadTimestamp
 * @property state
 * @property tempIdKarere
 * @property videoDownSampled
 * @property filePath
 * @property nodeHandle
 * @property fingerprint
 * @property name
 * @property transferTag
 */
data class SavePendingMessageRequest(
    val chatId: Long,
    val type: Int,
    val uploadTimestamp: Long,
    val state: PendingMessageState,
    val tempIdKarere: Long,
    val videoDownSampled: String?,
    val filePath: String,
    val nodeHandle: Long,
    val fingerprint: String?,
    val name: String?,
    val transferTag: Int,
)
