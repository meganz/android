package mega.privacy.android.app.psa

import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.usecase.psa.ClearPsaUseCase
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.psa.FetchPsaUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The ViewModel for PSA logic.
 */
@Singleton
class PsaManager @Inject constructor(
    private val fetchPsaUseCase: FetchPsaUseCase,
    private val clearPsaUseCase: ClearPsaUseCase,
    private val dismissPsaUseCase: DismissPsaUseCase,
) {

    /**
     * Check PSA from server
     */
    suspend fun checkPsa() {
        val psa = kotlin.runCatching { fetchPsaUseCase(System.currentTimeMillis()) }
            .onFailure { Timber.e(it) }
            .getOrNull()
        LiveEventBus.get(Constants.EVENT_PSA, Psa::class.java).post(psa)
    }

    /**
     * Clean Psa Check timestamp from preferences
     */
    suspend fun clearPsa() = clearPsaUseCase()


    /**
     * Dismiss the PSA.
     *
     * @param id the id of the PSA
     */
    suspend fun dismissPsa(id: Int) = dismissPsaUseCase(id)
}
