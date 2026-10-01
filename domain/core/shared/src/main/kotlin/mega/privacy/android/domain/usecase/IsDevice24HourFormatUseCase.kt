package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.TimeSystemRepository
import javax.inject.Inject

/**
 * Use case to check if the device is using 24-hour format
 */
class IsDevice24HourFormatUseCase @Inject constructor(private val timeSystemRepository: TimeSystemRepository) {
    /**
     * Invoke the use case
     */
    operator fun invoke(): Boolean = timeSystemRepository.is24HourFormat()
}
