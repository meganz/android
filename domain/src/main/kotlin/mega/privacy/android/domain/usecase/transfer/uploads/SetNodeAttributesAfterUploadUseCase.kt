package mega.privacy.android.domain.usecase.transfer.uploads

import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsPdfFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreatePdfPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreatePdfThumbnailUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case for setting node attributes after upload use case.
 *  - If video or image: Thumbnail, preview & coordinates.
 *  - If pdf: Thumbnail & preview.
 *
 * @property createImageOrVideoThumbnailUseCase [CreateImageOrVideoPreviewUseCase]
 * @property createImageOrVideoPreviewUseCase
 * @property setNodeCoordinatesUseCase
 * @property isVideoFileUseCase
 * @property isImageFileUseCase
 * @property isPdfFileUseCase
 * @property createPdfThumbnailUseCase
 * @property createPdfPreviewUseCase
 * @constructor Create empty Set node attributes after upload use case
 */
class SetNodeAttributesAfterUploadUseCase @Inject constructor(
    private val createImageOrVideoThumbnailUseCase: CreateImageOrVideoThumbnailUseCase,
    private val createImageOrVideoPreviewUseCase: CreateImageOrVideoPreviewUseCase,
    private val setNodeCoordinatesUseCase: SetNodeCoordinatesUseCase,
    private val isVideoFileUseCase: IsVideoFileUseCase,
    private val isImageFileUseCase: IsImageFileUseCase,
    private val isPdfFileUseCase: IsPdfFileUseCase,
    private val createPdfThumbnailUseCase: CreatePdfThumbnailUseCase,
    private val createPdfPreviewUseCase: CreatePdfPreviewUseCase,
) {

    /**
     * Invoke
     *
     * @param nodeHandle Node handle of the file already in the Cloud.
     * @param localFile Local file.
     */
    suspend operator fun invoke(nodeHandle: Long, localFile: File) {
        val localPath = localFile.absolutePath
        val isVideoOrImage = isVideoFileUseCase(localPath) || isImageFileUseCase(localPath)
        val isPdf = isPdfFileUseCase(localPath)

        if (isVideoOrImage) {
            createImageOrVideoThumbnailUseCase(nodeHandle = nodeHandle, localFile = localFile)
            createImageOrVideoPreviewUseCase(nodeHandle = nodeHandle, localFile = localFile)
            setNodeCoordinatesUseCase(localPath = localPath, nodeHandle = nodeHandle)
        } else if (isPdf) {
            createPdfThumbnailUseCase(nodeHandle = nodeHandle, localFile = localFile)
            createPdfPreviewUseCase(nodeHandle = nodeHandle, localFile = localFile)
        }
    }
}