package mega.privacy.android.domain.entity.node

import mega.privacy.android.domain.entity.FileTypeInfo

/**
 * File node
 */
interface FileNode : UnTypedNode {
    /**
     * Size
     */
    val size: Long

    /**
     * Modification time
     */
    val modificationTime: Long

    /**
     * Type
     */
    val type: FileTypeInfo

    /**
     * Thumbnail path
     */
    val thumbnailPath: String?

    /**
     * Preview path
     */
    val previewPath: String?

    /**
     * Full Size path
     */
    val fullSizePath: String?

    /**
     * Fingerprint
     */
    val fingerprint: String?


    /**
     * Original Fingerprint
     */
    val originalFingerprint: String?

    /**
     * Has thumbnail
     */
    val hasThumbnail: Boolean

    /**
     * Has preview
     */
    val hasPreview: Boolean
}
