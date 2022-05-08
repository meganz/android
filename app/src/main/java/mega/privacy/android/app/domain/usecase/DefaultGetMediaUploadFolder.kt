package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AlbumsRepository
import javax.inject.Inject

/**
 * The use case implementation class to get media upload folder handle String
 * @param repository AlbumsRepository
 */
class DefaultGetMediaUploadFolder @Inject constructor(
    private val repository: AlbumsRepository
) : GetMediaUploadFolder {

    override fun invoke(): String? = repository.getMediaUploadFolder()
}