package mega.privacy.android.domain.entity.chat

/**
 * Pending message
 *
 * @property id
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
 * @constructor Create empty Pending message
 */
data class PendingMessage(
    var id: Long = -1,
    var chatId: Long = -1,
    var type: Int = -1,
    var uploadTimestamp: Long = -1,
    var state: Int = PendingMessageState.PREPARING.value,
    var tempIdKarere: Long = -1,
    var videoDownSampled: String? = null,
    var filePath: String = "",
    var nodeHandle: Long = -1,
    var fingerprint: String? = null,
    var name: String? = null,
    var transferTag: Int = -1,
) {

    constructor(
        chatId: Long,
        uploadTimestamp: Long,
        filePath: String,
        fingerprint: String?,
        name: String?,
    ) : this() {
        this.chatId = chatId
        this.uploadTimestamp = uploadTimestamp
        this.filePath = filePath
        this.fingerprint = fingerprint
        this.name = name
    }

    constructor(
        id: Long,
        chatId: Long,
        uploadTimestamp: Long,
        tempIdKarere: Long,
        filePath: String,
        fingerprint: String?,
        name: String?,
        nodeHandle: Long,
        transferTag: Int,
        state: Int,
    ) : this() {
        this.id = id
        this.chatId = chatId
        this.uploadTimestamp = uploadTimestamp
        this.tempIdKarere = tempIdKarere
        this.filePath = filePath
        this.fingerprint = fingerprint
        this.name = name
        this.nodeHandle = nodeHandle
        this.transferTag = transferTag
        this.state = state
    }
}