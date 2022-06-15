package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import javax.inject.Inject

/**
 * Use case implementation for getting cloud sort order
 */
class DefaultGetCloudSortOrder @Inject constructor(
    private val repository: FilesRepository
) : GetCloudSortOrder {
    override suspend fun invoke(): Int = repository.getCloudSortOrder()
}