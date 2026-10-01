package mega.privacy.android.domain.usecase.imagepreview

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import javax.inject.Inject

/**
 * Use case to check whether a video file format can be edited by the video editor.
 */
class IsEditableVideoUseCase @Inject constructor() {

    /**
     * Check if the given file type is an editable video.
     *
     * @param fileType The [FileTypeInfo] to check.
     * @return true if the file is a video with a supported editable mime type.
     */
    operator fun invoke(fileType: FileTypeInfo): Boolean =
        fileType is VideoFileTypeInfo && fileType.mimeType in EDITABLE_VIDEO_MIME_TYPES

    companion object {
        private val EDITABLE_VIDEO_MIME_TYPES = setOf(
            // MP4 / M4V (also the container for MOV-style content)
            "video/mp4",
            "video/x-m4v",
            // QuickTime / MOV
            "video/quicktime",
            // Matroska / WebM
            "video/x-matroska",
            "video/webm",
            // 3GPP
            "video/3gpp",
            "video/3gpp2",
            // MPEG transport / program / elementary streams
            "video/mp2t",
            "video/mpeg",
            // AVI
            "video/avi",
            "video/x-msvideo",
            "video/msvideo",
            // FLV
            "video/x-flv",
        )
    }
}
