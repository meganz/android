package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileRepository
import javax.inject.Inject

/**
 * * Use Case for checking whether node is in rubbish or not
 *
 * @property Boolean
 */
class DefaultCheckNodeInRubbish @Inject constructor(
    private val fileRepository: FileRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) :
    CheckNodeInRubbish {
    override suspend fun invoke(handle: Long) =
        withContext(ioDispatcher) { fileRepository.checkNodeInRubbish(handle) }
}