package mega.privacy.android.domain.usecase.imagepreview

import mega.privacy.android.domain.entity.FileTypeInfo
import javax.inject.Inject

/**
 * Use case to check whether an image file format can be edited by the image editor.
 */
class IsEditableImageUseCase @Inject constructor() {

    /**
     * Check if the given file type is an editable image.
     *
     * @param fileType The [FileTypeInfo] to check.
     * @return true if the file's mime type is a supported editable image format.
     */
    operator fun invoke(fileType: FileTypeInfo): Boolean =
        fileType.mimeType in EDITABLE_IMAGE_MIME_TYPES

    companion object {
        private val EDITABLE_IMAGE_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/bmp",
            "image/x-ms-bmp",
            "image/heif",
        )
    }
}
