package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.TimeSystemRepository
import javax.inject.Inject

class GetCurrentTimeInMillisUseCase @Inject constructor(
    private val timeSystemRepository: TimeSystemRepository,
) {

    operator fun invoke() = timeSystemRepository.getCurrentTimeInMillis()
}