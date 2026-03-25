package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class FilterCameraUploadMediaUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    suspend operator fun invoke(source: List<TypedFileNode>) =
        createTempSyncFolderIds().let { sync ->
            source.filter { it.parentId.longValue in sync }
        }

    private suspend fun createTempSyncFolderIds() =
        listOfNotNull(
            photosRepository.getCameraUploadFolderId(),
            photosRepository.getMediaUploadFolderId()
        )
}
