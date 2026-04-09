package mega.privacy.android.domain.usecase.continuewhereleftoff

import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Updates the recently-used timestamp so the item surfaces at the top of the carousel.
 * Position data (playback progress, page number) is persisted by the respective
 * media/PDF repositories.
 */
class SavePositionUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    suspend operator fun invoke(nodeHandle: Long) = repository.savePosition(nodeHandle)
}
