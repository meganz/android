package mega.privacy.android.domain.repository.agesignal

import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus

/**
 * Defines the contract for fetching age signal compliance status.
 */
interface AgeSignalRepository {
    suspend fun fetchAgeSignal(): UserAgeComplianceStatus
}
