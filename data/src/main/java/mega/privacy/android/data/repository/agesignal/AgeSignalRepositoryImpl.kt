package mega.privacy.android.data.repository.agesignal

import android.content.Context
import com.google.android.play.agesignals.AgeSignalsException
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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
class AgeSignalRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : AgeSignalRepository {

    private val ageSignalsManager = AgeSignalsManagerFactory.create(context)

    override suspend fun fetchAgeSignal(): UserAgeComplianceStatus =
        withContext(defaultDispatcher) {
            runCatching {
                val request = AgeSignalsRequest.builder().build()

                // 1. Use coroutine extension to suspend and await the asynchronous result
                val result = ageSignalsManager.checkAgeSignals(request).result

                // 2. Map the Google Play API user status directly to our domain compliance status
                when (result.userStatus()) {
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
