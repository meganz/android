package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.FilesRepository
import javax.inject.Inject

/**
 * Default check if is pending share
 */
class DefaultIsPendingShare @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val filesRepository: FilesRepository,
) : IsPendingShare {

    override suspend fun invoke(handle: Long): Boolean =
        if (handle == -1L) false
        else getNodeByHandle(handle)?.let { filesRepository.isPendingShare(it) } ?: false
}