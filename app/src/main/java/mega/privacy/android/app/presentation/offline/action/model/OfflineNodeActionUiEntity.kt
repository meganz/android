package mega.privacy.android.app.presentation.offline.action.model

import mega.privacy.android.domain.entity.node.NodeId
import java.io.File

/**
 * Sealed class for offline file content
 */
sealed interface OfflineNodeActionUiEntity {

    /**
     * Image content
     * @property nodeId [NodeId]
     * @property path [String]
     */
    data class Image(
        val nodeId: NodeId,
        val path: String,
    ) : OfflineNodeActionUiEntity

    /**
     * Audio or video content
     * @property nodeId [NodeId]
     * @property name [String]
     * @property isSupported [Boolean]
     * @property isVideo [Boolean]
     * @property mimeType [String]
     * @property extension [String]
     * @property file [File]
     */
    data class AudioOrVideo(
        val nodeId: NodeId,
        val name: String,
        val isSupported: Boolean,
        val isVideo: Boolean,
        val mimeType: String,
        val extension: String,
        val file: File,
    ) : OfflineNodeActionUiEntity

    /**
     * Text content
     * @property file [File]
     */
    data class Text(
        val file: File,
    ) : OfflineNodeActionUiEntity

    /**
     * Zip content
     * @property nodeId [NodeId]
     * @property file [File]
     */
    data class Zip(
        val nodeId: NodeId,
        val file: File,
    ) : OfflineNodeActionUiEntity

    /**
     * Uri content
     * @property path [String]
     */
    data class Uri(
        val path: String?,
    ) : OfflineNodeActionUiEntity

    /**
     * Pdf content
     * @property nodeId [NodeId]
     * @property file [File]
     * @property mimeType [String]
     */
    data class Pdf(
        val nodeId: NodeId,
        val file: File,
        val mimeType: String,
    ) : OfflineNodeActionUiEntity

    /**
     * Other content
     * @property file [File]
     * @property mimeType [String]
     */
    data class Other(
        val file: File,
        val mimeType: String,
    ) : OfflineNodeActionUiEntity
}