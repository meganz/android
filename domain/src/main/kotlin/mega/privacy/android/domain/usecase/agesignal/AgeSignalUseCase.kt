package mega.privacy.android.domain.usecase.agesignal

import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus
import mega.privacy.android.domain.repository.agesignal.AgeSignalRepository
import javax.inject.Inject

/**
 * Use case for fetching user age compliance status.
 */
class AgeSignalUseCase @Inject constructor(
    private val repository: AgeSignalRepository,
) {
    suspend operator fun invoke(): UserAgeComplianceStatus = repository.fetchAgeSignal()
}
