package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.preferences.InAppUpdatePreferencesGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * [InAppUpdateRepository] Implementation
 *
 */
internal class InAppUpdateRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val inAppUpdatePreferencesGateway: InAppUpdatePreferencesGateway,
) : InAppUpdateRepository {

    override suspend fun setLastInAppUpdatePromptTime(time: Long) {
        withContext(ioDispatcher) {
            inAppUpdatePreferencesGateway.setLastInAppUpdatePromptTime(time)
        }
    }

    override suspend fun getLastInAppUpdatePromptTime(): Long = withContext(ioDispatcher) {
        inAppUpdatePreferencesGateway.getLastInAppUpdatePromptTime()
    }

    override suspend fun incrementInAppUpdatePromptCount() {
        withContext(ioDispatcher) {
            inAppUpdatePreferencesGateway.incrementInAppUpdatePromptCount()
        }
    }

    override suspend fun getInAppUpdatePromptCount(): Int = withContext(ioDispatcher) {
        inAppUpdatePreferencesGateway.getInAppUpdatePromptCount()
    }

    override suspend fun setInAppUpdatePromptCount(count: Int) {
        withContext(ioDispatcher) {
            inAppUpdatePreferencesGateway.setInAppUpdatePromptCount(count)
        }
    }

    override suspend fun getLastInAppUpdatePromptVersion(): Int = withContext(ioDispatcher) {
        inAppUpdatePreferencesGateway.getLastInAppUpdatePromptVersion()
    }

    override suspend fun setLastInAppUpdatePromptVersion(version: Int) {
        withContext(ioDispatcher) {
            inAppUpdatePreferencesGateway.setLastInAppUpdatePromptVersion(version)
        }
    }

}
