package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CancelTokenRepository
import javax.inject.Inject

/**
 * Default implementation of [CancelTokenRepository]
 */
internal class DefaultCancelTokenRepository @Inject constructor(
    private val cancelTokenProvider: CancelTokenProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CancelTokenRepository {
    override suspend fun cancelCurrentToken() = withContext(ioDispatcher) {
        cancelTokenProvider.cancelCurrentToken()
    }

    override suspend fun invalidateCurrentToken() = withContext(ioDispatcher) {
        cancelTokenProvider.invalidateCurrentToken()
    }
}