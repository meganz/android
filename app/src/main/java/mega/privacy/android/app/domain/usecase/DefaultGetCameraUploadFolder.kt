package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AlbumsRepository
import javax.inject.Inject

/**
 * The use case implementation class to get camera upload folder handle String
 * @param repository AlbumsRepository
 */
class DefaultGetCameraUploadFolder @Inject constructor(
    private val repository: AlbumsRepository
) : GetCameraUploadFolder {

    override fun invoke(): String? = repository.getCameraUploadFolder()
}