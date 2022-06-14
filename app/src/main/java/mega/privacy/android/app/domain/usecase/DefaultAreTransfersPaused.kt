package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.GlobalStatesRepository
import javax.inject.Inject

/**
 * Use case to check if download or upload transfers are paused.
 *
 * @property globalStatesRepository
 */
class DefaultAreTransfersPaused @Inject constructor(private val globalStatesRepository: GlobalStatesRepository) :
    AreTransfersPaused {

    /**
     * Are transfers paused (downloads and uploads)
     */
    override suspend operator fun invoke(): Boolean = globalStatesRepository.areTransfersPaused()
}
