package mega.privacy.android.domain.usecase.transfers.uploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsPdfFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreatePdfPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreatePdfThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPublicNodeThumbnailUseCase
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
    private val getPublicNodeThumbnailUseCase: GetPublicNodeThumbnailUseCase,
) {

    /**
     * Invoke
     *
     * @param nodeHandle Node handle of the file already in the Cloud.
     * @param uriPath Uri path.
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        uriPath: UriPath,
        appData: List<TransferAppData>?,
    ) {
        val isVideoOrImage = isVideoFileUseCase(uriPath) || isImageFileUseCase(uriPath)
        val isPdf = isPdfFileUseCase(uriPath)

        if (isVideoOrImage) {
            if (getPublicNodeThumbnailUseCase(nodeHandle) == null) {
                createImageOrVideoThumbnailUseCase(nodeHandle = nodeHandle, uriPath = uriPath)
                createImageOrVideoPreviewUseCase(nodeHandle = nodeHandle, uriPath = uriPath)
            }
            setNodeCoordinatesUseCase(
                nodeHandle = nodeHandle,
                uriPath = uriPath,
                geolocation = appData?.filterIsInstance<TransferAppData.Geolocation>()
                    ?.firstOrNull(),
            )
        } else if (isPdf) {
            createPdfThumbnailUseCase(nodeHandle = nodeHandle, uriPath = uriPath)
            createPdfPreviewUseCase(nodeHandle = nodeHandle, uriPath = uriPath)
        }
    }
}