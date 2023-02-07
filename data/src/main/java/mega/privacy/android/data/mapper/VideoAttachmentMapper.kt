package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.VideoAttachment
import java.io.File

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
    records.filter { it.localPath.isNullOrEmpty().not() && it.newPath.isNullOrEmpty().not() }.map {
        VideoAttachment(it.localPath!!, it.newPath!!, File(it.localPath!!).length(), null, it.id)
    }
