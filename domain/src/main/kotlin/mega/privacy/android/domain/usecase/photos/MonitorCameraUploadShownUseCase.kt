package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class MonitorCameraUploadShownUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    val cameraUploadShownFlow: Flow<Boolean> = photosRepository.cameraUploadShownFlow
}
