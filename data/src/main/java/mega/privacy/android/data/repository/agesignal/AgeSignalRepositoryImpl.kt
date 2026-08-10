package mega.privacy.android.data.repository.agesignal

import com.google.android.play.agesignals.AgeSignalsException
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.AgeSignalsGateway
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus.AdultVerified
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus.RequiresMinorRestriction
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.agesignal.AgeSignalRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Concrete implementation of the AgeSignalRepository.
 *
 * Responsibility: Bridge the external Google Play Age Signals SDK with our internal
 * data and domain models. Contains the critical logic for API calls and error mapping.
 *
 * Note: The Play Age Signals API (beta) will throw exceptions until January 1, 2026.
 * From January 1, 2026, the API will return live responses. All exceptions are handled
 * by defaulting to RequiresMinorRestriction for legal compliance.
 */
internal class AgeSignalRepositoryImpl @Inject constructor(
    private val ageSignalsGateway: AgeSignalsGateway,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : AgeSignalRepository {

    override suspend fun fetchAgeSignal(): UserAgeComplianceStatus =
        withContext(defaultDispatcher) {
            runCatching {
                // 1. Use suspend function to get the user status
                val userStatus = ageSignalsGateway.checkAgeSignals()

                // 2. Map the Google Play API user status directly to our domain compliance status
                when (userStatus) {
                    AgeSignalsVerificationStatus.VERIFIED -> AdultVerified

                    // All non-VERIFIED states mandate the restrictive flow for compliance.
                    else -> RequiresMinorRestriction
                }
            }
                .onFailure { exception ->
                    when (exception) {
                        // 3. Handle expected API-level exceptions
                        is AgeSignalsException -> {
                            Timber.e(
                                exception,
                                "Failed to fetch age signal: ${exception.errorCode}"
                            )
                        }

                        // 4. Handle unexpected exceptions (e.g., coroutine cancellation, general I/O)
                        else -> {
                            Timber.e(exception, "Unexpected error while fetching age signal")
                        }
                    }
                }
                .getOrDefault(RequiresMinorRestriction)
        }
}
