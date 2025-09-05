package mega.privacy.android.domain.usecase.advertisements

import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Use case to set Google consent loaded state
 */
class SetGoogleConsentLoadedUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
) {
    /**
     * Invoke
     * @param isLoaded [Boolean] true if Google consent has been loaded, false otherwise
     */
    operator fun invoke(isLoaded: Boolean) = adsRepository.setGoogleConsentLoaded(isLoaded)
}
