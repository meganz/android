package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class SetCameraUploadShownUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    suspend operator fun invoke() {
        photosRepository.setCameraUploadShown()
    }
}
