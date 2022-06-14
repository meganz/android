package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import javax.inject.Inject

/**
 * Use case implementation for getting camera sort order
 */
class DefaultGetCameraSortOrder @Inject constructor(
    private val repository: FilesRepository
) : GetCameraSortOrder {
    override suspend fun invoke(): Int = repository.getCameraSortOrder()
}