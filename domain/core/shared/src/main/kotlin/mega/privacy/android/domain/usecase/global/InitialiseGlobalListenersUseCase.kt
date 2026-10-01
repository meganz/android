package mega.privacy.android.domain.usecase.global

import mega.privacy.android.domain.repository.InitializationRepository
import javax.inject.Inject

/**
 * Initialise global listeners use case
 * Global request listener (Old BackgroundRequestListener)
 * Global listener TBD
 *
 * @property initializationRepository
 */
class InitialiseGlobalListenersUseCase @Inject constructor(
    private val initializationRepository: InitializationRepository,
) {
    suspend operator fun invoke() {
        initializationRepository.initializeGlobalRequestListener()
    }
}