package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.VideoAttachment

/**
 * Mapper to convert list of sync records to video attachment
 */
typealias VideoAttachmentMapper = (@JvmSuppressWildcards List<@JvmSuppressWildcards SyncRecord>) -> @JvmSuppressWildcards List<@JvmSuppressWildcards VideoAttachment>

/**
 * SyncRecord to Video Attachment
 *
 * @param records
 */
internal fun toVideoAttachment(records: List<@JvmSuppressWildcards SyncRecord>) =
    records.filter { it.localPath.isEmpty().not() && it.newPath.isNullOrEmpty().not() }.map {
        VideoAttachment(it.localPath, it.newPath!!, id = it.id.toLong(), pendingMessageId = null)
    }
