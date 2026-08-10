package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.model.ZipFileTypedNode
import java.io.File
import javax.inject.Inject

/**
 * Mapper to convert [File] to [ZipFileTypedNode]
 *
 * Used when building a synthetic node for a file inside a zip archive,
 * e.g. when the video player is launched from a zip file adapter.
 */
class ZipFileTypedNodeMapper @Inject constructor() {

    /**
     * Convert a [File] to [ZipFileTypedNode]
     *
     * @param file The local file inside the zip archive
     * @return [ZipFileTypedNode] representing the file
     */
    operator fun invoke(file: File): ZipFileTypedNode = ZipFileTypedNode(file)
}
