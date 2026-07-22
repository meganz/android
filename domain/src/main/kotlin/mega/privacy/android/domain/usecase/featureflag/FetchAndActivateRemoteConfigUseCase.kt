package mega.privacy.android.domain.usecase.featureflag

import mega.privacy.android.domain.repository.RemoteConfigRepository
import javax.inject.Inject

/**
 * Use case to fetch the latest Firebase Remote Config values and activate them
 */
class FetchAndActivateRemoteConfigUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    /**
     * Invoke
     *
     * @param useMinimalFetchInterval If true, bypass the default fetch throttling
     * @return true if the fetched values were activated for this app instance
     */
    suspend operator fun invoke(useMinimalFetchInterval: Boolean = false): Boolean =
        remoteConfigRepository.fetchAndActivate(useMinimalFetchInterval)
}
