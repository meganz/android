package mega.privacy.android.domain.usecase.inappupdate

import mega.privacy.android.domain.repository.InAppUpdateRepository
import javax.inject.Inject

/**
 * Set InAppUpdate Never Show Again Use case
 */
class SetInAppUpdateNeverShowAgainUseCase @Inject constructor(
    private val inAppUpdateRepository: InAppUpdateRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(value: Boolean) =
        inAppUpdateRepository.setInAppUpdateNeverShowAgain(value)
}